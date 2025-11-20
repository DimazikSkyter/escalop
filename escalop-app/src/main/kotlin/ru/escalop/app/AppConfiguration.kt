package ru.escalop.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.server.application.*
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import ru.escalop.app.huggingface.HuggingFaceClientImpl
import ru.escalop.app.properties.HuggingFaceClientProperties
import ru.escalop.app.service.DataUnionService
import ru.escalop.app.service.HealthDataService
import ru.escalop.app.service.HealthDataServiceImpl
import ru.escalop.app.storage.StorageNamesCoordinator
import ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.common.model.User
import ru.escalop.common.properties.YandexDiskStorageProperties
import ru.escalop.common.remotestorage.RemoteStorage
import ru.escalop.common.remotestorage.YandexDiskRemoteStorage
import ru.escalop.common.repositories.*
import ru.escalop.common.secure.SecureTokenManager
import javax.sql.DataSource


val appModule = module {

    // инфраструктура
    single { HttpClient() }  // общий httpClient для всего, если ок

    single { User("name") } // TODO: потом заменить на реального юзера/токен

    single { SecureTokenManager() }

    single { YandexDiskStorageProperties("/escalop/") }

    single<RemoteStorage> {
        // да, тут блокирующий вызов, но он выполняется один раз при старте
        runBlocking {
            YandexDiskRemoteStorage.create(
                client = get(),
                user = get(),
                secureTokenManager = get(),
                properties = get()
            )
        }
    }

    // HuggingFace
    single { HuggingFaceClientProperties(baseUrl = "http://localhost:7860/", apiToken = "") }

    single<FileMetricExtractorClient> {
        val props: HuggingFaceClientProperties = get()
        HuggingFaceClientImpl(
            httpClient = get(),
            baseUrl = props.baseUrl.trimEnd('/'),
            apiToken = props.apiToken,
            endpointPath = "/extract", // можно не указывать, если в классе уже дефолт /extract
        )
    }

    // Сервисы домена
    single { DataUnionService() }

    single { StorageNamesCoordinator() }

    // Репозитории (EntityManager где-то рядом должен жить, см. ниже)
    single<SnapshotRepository> { SnapshotRepositoryImpl(get<EntityManager>()) }
    single<SnapshotSourceRepository> { SnapshotSourceRepositoryImpl(get<EntityManager>()) }
    single<AnalysisTypeRepository> { AnalysisTypeRepositoryImpl(get<EntityManager>()) }
    single<UserRepository> { UserRepositoryImpl(get<EntityManager>()) }

    // Главный сервис
    single<HealthDataService> {
        HealthDataServiceImpl(
            dataUnionService = get(),
            remoteStorage = get(),
            fileMetricExtractorClient = get(),
            storageNamesCoordinator = get(),
            snapshotRepository = get(),
            snapshotSourceRepository = get(),
            analysisTypeRepository = get(),
            userRepository = get()
        )
    }
}

val jpaModule = module {

    // DataSource как singleton
    single<HikariDataSource> {
        val env: ApplicationEnvironment = get()

        val dbConfig = env.config.config("ktor.db")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.property("jdbcUrl").getString()
            driverClassName = dbConfig.property("driverClassName").getString()
            username = dbConfig.property("username").getString()
            password = dbConfig.property("password").getString()
            maximumPoolSize = 5
        }

        HikariDataSource(hikariConfig)
    }

    single<DataSource> {
        get<HikariDataSource>()
    }

    // EntityManagerFactory — singleton
    single {
        val dataSource: DataSource = get()

        Persistence.createEntityManagerFactory(
            "default",
            mapOf(
                "jakarta.persistence.nonJtaDataSource" to dataSource,
                "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.hbm2ddl.auto" to "validate"
            )
        )
    }

    // Упрощённо: один EntityManager
    single<EntityManager> {
        get<EntityManagerFactory>().createEntityManager()
    }
}