package ru.escalop.app.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import ru.escalop.app.storage.StorageNamesCoordinator
import ru.escalop.common.dto.GetDataResponse
import ru.escalop.common.dto.UploadDocumentResponse
import ru.escalop.common.dto.UploadHistoryItem
import ru.escalop.common.entity.AnalysisTypeEntity
import ru.escalop.common.entity.Snapshot
import ru.escalop.common.entity.SnapshotSource
import ru.escalop.common.entity.UserEntity
import ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.common.model.*
import ru.escalop.common.remotestorage.RemoteStorage
import ru.escalop.common.repositories.AnalysisTypeRepository
import ru.escalop.common.repositories.SnapshotRepository
import ru.escalop.common.repositories.SnapshotSourceRepository
import ru.escalop.common.repositories.UserRepository
import java.time.LocalDate
import java.util.*
import kotlin.math.log

interface HealthDataService {

    //todo за год, за файл, конкретный тип, конкретная метрика
    fun getDataByFilter(user: User, filter: Filter): GetDataResponse

    fun uploadNewDocument(user: User, document: Document): UploadDocumentResponse

    fun getUploadHistory(user: User): List<UploadHistoryItem>
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
        val snapshots: List<Snapshot> = snapshotRepository.findByFilterAndUser(filter, login)
        val fileNames: List<String> = snapshots.map {
            it.source?.let { source ->
                storageNamesCoordinator.generateStorageName(
                    it.analysisTypeEntity.toModel(), source.year, source.index
                )
            }
        }.filterNotNull()

        logger.debug("Try to extract files {} for user {}", fileNames, user.name)

        val healthParts: List<StorageHealthPart> = fileNames.map {
            val result: String? = runBlocking {
                try {
                    withTimeout(60000) {
                        remoteStorage.readData(user, it)
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
            healthParts
        )
    }

    //todo добавить стейты
    override fun uploadNewDocument(user: User, document: Document): UploadDocumentResponse {
        logger.info("Upload new document ${document.name} for user ${user.name}")
        val userEntity: UserEntity = userRepository.findByLogin(user.name)
            ?: throw RuntimeException("User ${user.name} does not exist")
        var parsedHealthData: HealthDataResult? = null
        //получили новый документ
        runBlocking {
            try {
                withTimeout(5 * 60000) {
                    parsedHealthData = fileMetricExtractorClient.parseHealthData(document.name, LocalDate.now(), document.body)
                    //LocalDate.now() Поменять на правильную
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
            var snapshotSource: SnapshotSource = SnapshotSource(newSnapshot.id, newSnapshot, year, 1)
//            snapshotSource = snapshotSourceRepository.save(snapshotSource)
            snapshotSource
        } else {
            val newSnapshot: Snapshot = saveNewSnapshot(document.name, parsedHealthData!!, userEntity)
            val source: SnapshotSource = snapshotSourceRepository.getSourceBySnapshot(snapshots.last())
            snapshotSourceRepository.save(SnapshotSource(newSnapshot.id, newSnapshot, year, source.index))
            runBlocking {
                remoteStorage.readData(
                    user,
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
                return@runBlocking withTimeout(5 * 60000) {
                    saveInFile(
                        user,
                        healthDataResults,
                        parsedHealthData!!.analysisType,
                        snapshotSource
                    )
                    return@withTimeout UploadDocumentResponse(200, "Success", null)
                }
            } catch (e: Exception) {
                logger.error("Failed to save data", e)
                return@runBlocking UploadDocumentResponse(500, null, "Failed to save data")
            }
        }
        //сохранили в удаленный источник
    }

    override fun getUploadHistory(user: User): List<UploadHistoryItem> {
        logger.info("getUploadHistory() called for user '{}'", user.name)

        val userEntity: UserEntity = userRepository.findByLogin(user.name)
            ?: throw RuntimeException("User ${user.name} does not exist")

        logger.info("DB userEntity id={} login={}", userEntity.id, userEntity.login)

        val snapshots = snapshotRepository.findHistoryByUser(userEntity)
        logger.info("Found {} snapshots for user {}", snapshots.size, user.name)

        return snapshots.map { snapshot ->
            val fileName = snapshot.documentName
            val format = fileName.substringAfterLast('.', "").lowercase().ifBlank { "unknown" }

            UploadHistoryItem(
                fileName = fileName,
                format = format,
                date = snapshot.localDate?.toString(),
                analysisType = snapshot.analysisTypeEntity.code
            )
        }
    }

    private fun saveNewSnapshot(
        documentName: String,
        parsedHealthData: HealthDataResult,
        userEntity: UserEntity
    ): Snapshot {
        val analysisType: AnalysisTypeEntity = analysisTypeRepository.getByName(parsedHealthData.analysisType.name)
            ?: throw RuntimeException(
                "Failed to find correct analysisType, income is ${parsedHealthData.analysisType.name}"
            )
        return snapshotRepository.save(
            Snapshot(
                null,
                userEntity,
                analysisType,
                parsedHealthData.date,
                documentName,
                parsedHealthData.metrics.map { it.name }.joinToString()
            )
        )
    }

    private suspend fun saveInFile(
        user: User,
        healthDataResults: List<HealthDataResult>,
        analysisType: AnalysisType,
        source: SnapshotSource
    ) {
        val storageData = StorageHealthPart.fromHealthDataResult(
            healthDataResults,
            analysisType.name,
            source
        )
        val path: String = storageNamesCoordinator.generateStorageName(analysisType, source.year, source.index)
        remoteStorage.writeData(user, path, storageData.saveAsJson())
    }

    private fun currentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

}