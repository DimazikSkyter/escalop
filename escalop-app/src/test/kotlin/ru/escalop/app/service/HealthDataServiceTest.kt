package ru.escalop.app.service

import io.mockk.every
import io.mockk.mockk
import ru.escalop.app.storage.StorageNamesCoordinator
import ru.escalop.common.dto.GetDataResponse
import ru.escalop.common.entity.AnalysisTypeEntity
import ru.escalop.common.entity.Snapshot
import ru.escalop.common.entity.UserEntity
import ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.common.model.AnalysisType
import ru.escalop.common.model.Filter
import ru.escalop.common.model.User
import ru.escalop.common.remotestorage.RemoteStorage
import ru.escalop.common.repositories.AnalysisTypeRepository
import ru.escalop.common.repositories.SnapshotRepository
import ru.escalop.common.repositories.SnapshotSourceRepository
import ru.escalop.common.repositories.UserRepository
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test

class HealthDataServiceTest {

    private val fileName = "file name"
    private val localDate = LocalDate.of(2025, 10, 1)

    lateinit var healthDataService: HealthDataService

    @BeforeTest
    fun init() {
        val login = "login"

        val dataUnionService: DataUnionService = mockk<DataUnionService>()
        val remoteStorage: RemoteStorage = mockk<RemoteStorage>()
        val fileMetricExtractorClient: FileMetricExtractorClient = mockk<FileMetricExtractorClient>()
        val storageNamesCoordinator: StorageNamesCoordinator = mockk<StorageNamesCoordinator>()
        val snapshotRepository: SnapshotRepository = mockk<SnapshotRepository>()
        val snapshotSourceRepository: SnapshotSourceRepository = mockk<SnapshotSourceRepository>()
        val analysisTypeRepository: AnalysisTypeRepository = mockk<AnalysisTypeRepository>()
        val userRepository: UserRepository = mockk<UserRepository>()

        val userEntity: UserEntity = UserEntity(
            1,
            "123",
            login,
            "password"
        )
        val snapshots: List<Snapshot> = listOf(
            Snapshot(
                1,
                userEntity,
                AnalysisTypeEntity(1,
                    AnalysisType.BLOOD_GENERAL.name,
                    "Some description"),
                localDate,
                fileName,
                "[\"aaa\", \"aab\", \"bca\", \"cab\", \"cca\"]"
            )
        )
        every { userRepository.findByLogin(any<String>()) } returns userEntity
        every { snapshotRepository.findByFilterAndUser(any<Filter>(), userEntity) } returns snapshots

        healthDataService = HealthDataServiceImpl(
            dataUnionService,
            remoteStorage,
            fileMetricExtractorClient,
            storageNamesCoordinator,
            snapshotRepository,
            snapshotSourceRepository,
            analysisTypeRepository,
            userRepository
        )
    }

    @Test
    fun getDataByFilterPositiveFlow() {
        val user: User = User("test name")
        val filter: Filter = Filter(
            fileName,
            AnalysisType.BLOOD_GENERAL,
            localDate,
            2025
        )

        val data: GetDataResponse = healthDataService.getDataByFilter(user, filter)

        println(data)
    }

    @Test
    fun uploadNewDocumentPositiveFlow() {

    }
}