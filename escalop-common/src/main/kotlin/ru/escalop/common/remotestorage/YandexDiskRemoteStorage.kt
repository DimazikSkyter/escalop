package ru.escalop.common.remotestorage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.escalop.common.model.User
import ru.escalop.common.properties.YandexDiskStorageProperties
import ru.escalop.common.secure.SecureToken
import ru.escalop.common.secure.SecureTokenManager

class YandexDiskRemoteStorage private constructor(
    val client: HttpClient,
    @Transient
    private val secureTokenManager: SecureTokenManager,
    private val properties: YandexDiskStorageProperties,
) : RemoteStorage {

    private suspend fun checkConnectionAndFolder(user: User) {
        val path: String = properties.escalopPath
        val authHeader: String = "OAuth ${secureTokenManager.getTokenByUser(user).asString()}"
        val meta = client.get("https://cloud-api.yandex.net/v1/disk/resources") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("path", path)
        }
        when (meta.status.value) {
            200 -> return
            404 -> {
                val create = client.put("https://cloud-api.yandex.net/v1/disk/resources") {
                    header(HttpHeaders.Authorization, authHeader)
                    parameter("path", path)
                }
                if (create.status == HttpStatusCode.Created || create.status == HttpStatusCode.Conflict) {
                    return
                }
                throw RuntimeException("Create dir failed: ${create.status} ${create.bodyAsText()}")
            }

            else -> {
                throw RuntimeException("Create dir failed: ${meta.status} ${meta.bodyAsText()}")
            }
        }
    }

    override suspend fun readData(user: User, path: String): String {
        val fullPath: String = properties.escalopPath + path
        val userToken: String = secureTokenManager.getTokenByUser(user).asString()
        val href = getDownloadHref(userToken, fullPath, false)
        return client.get(href).let {
            val body: String = it.bodyAsText()
            if (it.status.isSuccess()) {
                body
            } else {
                throw RuntimeException("Download failed: ${it.status} $body")
            }
        }
    }

    override suspend fun writeData(user: User, path: String, data: ByteArray) {
        val fullPath: String = properties.escalopPath + path
        val userToken: String = secureTokenManager.getTokenByUser(user).asString()
        val href = getUploadHref(userToken, fullPath, true)

        val putResp = client.put(href) { setBody(data) }
        val putBody = putResp.bodyAsText()
        if (putResp.status.isSuccess()) {
            return validate(userToken, fullPath)
        }
        throw RuntimeException("Upload failed: ${putResp.status} $putBody")
    }

    private suspend fun validate(userToken: String, fullPath: String) {
        val metaResp = client.get("https://cloud-api.yandex.net/v1/disk/resources") {
            header(HttpHeaders.Authorization, "OAuth $userToken")
            parameter("path", fullPath)
        }
        if (metaResp.status.isSuccess()) {
            return
        }

        throw RuntimeException("Meta check failed: ${metaResp.status} ${metaResp.bodyAsText()}")
    }

    private suspend fun getDownloadHref(userToken: String, path: String, overwrite: Boolean?): String {
        return getHref(userToken, path, overwrite, "download")
    }

    private suspend fun getUploadHref(userToken: String, path: String, overwrite: Boolean?): String {
        return getHref(userToken, path, overwrite, "upload")
    }

    private suspend fun getHref(userToken: String, path: String, overwrite: Boolean?, suffix: String): String {
        val resp = client.get("https://cloud-api.yandex.net/v1/disk/resources/$suffix") {
            header(HttpHeaders.Authorization, "OAuth $userToken")
            parameter("path", path)
            overwrite?.let { parameter("overwrite", it) }
        }
        val text = resp.bodyAsText()
        if (!resp.status.isSuccess()) {
            throw RuntimeException("Get download href failed: ${resp.status} $text")
        }
        val href = Json.parseToJsonElement(text).jsonObject["href"]?.jsonPrimitive?.content
        require(!href.isNullOrBlank()) { "download href not found in response" }
        return href
    }

    companion object {
        suspend fun create(
            client: HttpClient,
            secureTokenManager: SecureTokenManager,
            properties: YandexDiskStorageProperties
        ): YandexDiskRemoteStorage {
            return YandexDiskRemoteStorage(
                client,
                secureTokenManager,
                properties
            )
        }
    }
}