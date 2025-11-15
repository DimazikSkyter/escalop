package ru.escalop.common.remotestorage

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import ru.escalop.ru.escalop.common.model.User
import ru.escalop.ru.escalop.common.properties.YandexDiskStorageProperties
import ru.escalop.ru.escalop.common.remotestorage.YandexDiskRemoteStorage
import ru.escalop.ru.escalop.common.secure.SecureToken
import ru.escalop.ru.escalop.common.secure.SecureTokenManager
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class YandexDiskRemoteStorageTest {
    lateinit var remoteStorage: YandexDiskRemoteStorage

    @BeforeTest
    fun init() {
        val client = HttpClient(CIO) { expectSuccess = false }

        val token: String? = System.getenv("YANDEX_DISK_TOKEN")
        assertNotNull(token)
        val secureToken = SecureToken(token.toByteArray())

        val yandexDiskStorageProperties = YandexDiskStorageProperties("/escalop/")
        val user: User = User("Some name")

        val secureTokenManager: SecureTokenManager = object : SecureTokenManager() {
            override suspend fun getTokenByUser(user: User): SecureToken {
                return secureToken
            }
        } as SecureTokenManager

        runBlocking {
            remoteStorage = YandexDiskRemoteStorage.create(client, user, secureTokenManager, yandexDiskStorageProperties, )
        }
    }

    @Test
    fun testCreateFile() {
        runBlocking {
            remoteStorage.writeData("somefile.json", "{\"employees\": [{\"firstName\": \"John\", \"lastName\": \"Doe\"}, {\"firstName\": \"Anna\", \"lastName\": \"Smith\"}]}")
        }
    }
}