package ru.escalop.app.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import ru.escalop.app.storage.StorageNamesCoordinator
import ru.escalop.ru.escalop.common.dto.GetDataResponse
import ru.escalop.ru.escalop.common.dto.UploadDocumentResponse
import ru.escalop.ru.escalop.common.entity.AnalysisTypeEntity
import ru.escalop.ru.escalop.common.entity.Snapshot
import ru.escalop.ru.escalop.common.entity.SnapshotSource
import ru.escalop.ru.escalop.common.entity.UserEntity
import ru.escalop.ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.ru.escalop.common.model.*
import ru.escalop.ru.escalop.common.remotestorage.RemoteStorage
import ru.escalop.ru.escalop.common.repositories.AnalysisTypeRepository
import ru.escalop.ru.escalop.common.repositories.SnapshotRepository
import ru.escalop.ru.escalop.common.repositories.SnapshotSourceRepository
import ru.escalop.ru.escalop.common.repositories.UserRepository
import java.util.*

interface HealthDataService {

    //todo за год, за файл, конкретный тип, конкретная метрика
    fun getDataByFilter(user: User, filter: Filter): GetDataResponse

    fun uploadNewDocument(user: User, document: Document): UploadDocumentResponse
}

class HealthDataServiceImpl(
    private val dataUnionService: DataUnionService,
    private val remoteStorage: RemoteStorage,
    private val fileMetricExtractorClient: FileMetricExtractorClient,
    private val storageNamesCoordinator: StorageNamesCoordinator,
    private val snapshotRepository: SnapshotRepository,
    private val snapshotSourceRepository: SnapshotSourceRepository,
    private val analysisTypeRepository: AnalysisTypeRepository,
    private val userRepository: UserRepository
) : HealthDataService {

    val logger = LoggerFactory.getLogger(HealthDataService::class.java)!!
    val objectMapper: ObjectMapper = ObjectMapper()

    override fun getDataByFilter(user: User, filter: Filter): GetDataResponse {
        val login = userRepository.findByLogin(user.name) ?: throw RuntimeException("User ${user.name} does not exist")
        val snapshots = snapshotRepository.findByFilterAndUser(filter, login)
        val fileNames: List<String> = snapshots.map { storageNamesCoordinator.generateStorageName(
            it.analysisTypeEntity.toModel(), it.source!!.year, it.source!!.index) }

        logger.debug("Try to extract files {} for user {}", fileNames, user.name)

        val healthParts: List<StorageHealthPart> = fileNames.map {
            val result: String? = runBlocking {
                try {
                    withTimeout(60000) {
                        remoteStorage.readData(it)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to read data", e)
                    null
                }
            }
            objectMapper.readValue(result, StorageHealthPart::class.java) ?: null
        }.filterNotNull()

        return GetDataResponse.fromHealthDataResult(
            200,
            "Result with required filters",
            healthParts)
    }

    //todo добавить стейты
    override fun uploadNewDocument(user: User, document: Document): UploadDocumentResponse {
        val userEntity: UserEntity = userRepository.findByLogin(user.name)
            ?: throw RuntimeException("User ${user.name} does not exist")
        var parsedHealthData: HealthDataResult? = null
        //получили новый документ
        runBlocking {
            try {
                withTimeout(60000) {
                    parsedHealthData = fileMetricExtractorClient.parseHealthData(document.name, document.body)
                }
            } catch (e: Exception) {
                logger.error("Failed to extract metrics from file extractor service", e)
            }
        }
        if (parsedHealthData == null || parsedHealthData!!.metrics.isEmpty()) {
            return UploadDocumentResponse(500, null, "Failed to extract data from file")
        }
        //распарсили снапшот
        val year = parsedHealthData?.date?.year ?: currentYear()
        val snapshots = snapshotRepository.findByYearAndType(year, parsedHealthData!!.analysisType, userEntity)
        val healthDataResults: MutableList<HealthDataResult> = mutableListOf()
        val snapshotSource: SnapshotSource = if (snapshots.isEmpty()) {
            val newSnapshot: Snapshot = saveNewSnapshot(document.name, parsedHealthData!!, userEntity)
            val snapshotSource: SnapshotSource = snapshotSourceRepository
                .save(SnapshotSource(newSnapshot.id, newSnapshot, year, 1))
            snapshotSource
        } else {
            val newSnapshot: Snapshot = saveNewSnapshot(document.name, parsedHealthData!!, userEntity)
            val source: SnapshotSource = snapshotSourceRepository.getSourceBySnapshot(snapshots.last())
            snapshotSourceRepository.save(SnapshotSource(newSnapshot.id, newSnapshot, year, source.index))
            runBlocking {
                remoteStorage.readData(
                    storageNamesCoordinator.generateStorageName(
                        parsedHealthData!!.analysisType,
                        source.year,
                        source.index
                    )
                )?.let {
                    healthDataResults.addAll(StorageHealthPart.fromString(it).metrics)
                }
            }
            source
        }
        healthDataResults.add(parsedHealthData!!)
        return runBlocking {
            try {
                return@runBlocking withTimeout(60000) {
                    saveInFile(healthDataResults,
                        parsedHealthData!!.analysisType,
                        snapshotSource)
                    return@withTimeout UploadDocumentResponse(200, "Success", null)
                }
            } catch (e: Exception) {
                logger.error("Failed to save data", e)
                return@runBlocking UploadDocumentResponse(500, null,  "Failed to save data")
            }
        }
        //сохранили в удаленный источник
    }

    private fun saveNewSnapshot(documentName: String,
                                parsedHealthData: HealthDataResult,
                                userEntity: UserEntity): Snapshot {
        val analysisType: AnalysisTypeEntity = analysisTypeRepository.getByName(parsedHealthData.analysisType.name)
            ?: throw RuntimeException(
                "Failed to find correct analysisType, income is ${parsedHealthData.analysisType.name}")
        return snapshotRepository.save(Snapshot(
            null,
            analysisType,
            parsedHealthData.date,
            documentName,
            userEntity,
            parsedHealthData.metrics.map { it.name }.joinToString())
        )
    }

    private suspend fun saveInFile(
                           healthDataResults: List<HealthDataResult>,
                           analysisType: AnalysisType,
                           source: SnapshotSource) {
        val storageData = StorageHealthPart.fromHealthDataResult(
            healthDataResults,
            analysisType.name,
            source
        )
        val path: String = storageNamesCoordinator.generateStorageName(analysisType, source.year, source.index)
        remoteStorage.writeData(path, storageData.saveAsJson())
    }

    private fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

}