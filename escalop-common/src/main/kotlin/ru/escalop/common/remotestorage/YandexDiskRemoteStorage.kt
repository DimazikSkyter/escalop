package ru.escalop.ru.escalop.common.remotestorage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.escalop.ru.escalop.common.model.User
import ru.escalop.ru.escalop.common.properties.YandexDiskStorageProperties
import ru.escalop.ru.escalop.common.secure.SecureToken
import ru.escalop.ru.escalop.common.secure.SecureTokenManager

class YandexDiskRemoteStorage private constructor(
    val client: HttpClient,
    @Transient
    private val userToken: SecureToken,
    private val properties: YandexDiskStorageProperties,
    user: User
) : RemoteStorage {

    private suspend fun checkConnectionAndFolder(): YandexDiskRemoteStorage {
        val path: String = properties.escalopPath
        val authHeader: String = "OAuth ${userToken.asString()}"
        val meta = client.get("https://cloud-api.yandex.net/v1/disk/resources") {
            header(HttpHeaders.Authorization, authHeader)
            parameter("path", path)
        }
        when (meta.status.value) {
            200 -> return this
            404 -> {
                val create = client.put("https://cloud-api.yandex.net/v1/disk/resources") {
                    header(HttpHeaders.Authorization, authHeader)
                    parameter("path", path)
                }
                if (create.status == HttpStatusCode.Created || create.status == HttpStatusCode.Conflict) {
                    return this
                }
                throw RuntimeException("Create dir failed: ${create.status} ${create.bodyAsText()}")
            }

            else -> {
                throw RuntimeException("Create dir failed: ${meta.status} ${meta.bodyAsText()}")
            }
        }
    }

    override suspend fun readData(path: String): String {
        val fullPath: String = properties.escalopPath + path
        val href = getDownloadHref(fullPath, false)
        return client.get(href).let {
            val body: String = it.bodyAsText()
            if (it.status.isSuccess()) {
                body
            } else {
                throw RuntimeException("Download failed: ${it.status} $body")
            }
        }
    }

    override suspend fun writeData(path: String, data: ByteArray) {
        val fullPath: String = properties.escalopPath + path
        val href = getUploadHref(fullPath, true)

        val putResp = client.put(href) { setBody(data) }
        val putBody = putResp.bodyAsText()
        if (putResp.status.isSuccess()) {
            return validate(fullPath)
        }
        throw RuntimeException("Upload failed: ${putResp.status} $putBody")
    }

    private suspend fun validate(fullPath: String) {
        val metaResp = client.get("https://cloud-api.yandex.net/v1/disk/resources") {
            header(HttpHeaders.Authorization, "OAuth ${userToken.asString()}")
            parameter("path", fullPath)
        }
        if (metaResp.status.isSuccess()) {
            return
        }

        throw RuntimeException("Meta check failed: ${metaResp.status} ${metaResp.bodyAsText()}")
    }

    private suspend fun getDownloadHref(path: String, overwrite: Boolean?): String {
        return getHref(path, overwrite, "download")
    }

    private suspend fun getUploadHref(path: String, overwrite: Boolean?): String {
        return getHref(path, overwrite, "upload")
    }

    private suspend fun getHref(path: String, overwrite: Boolean?, suffix: String): String {
        val resp = client.get("https://cloud-api.yandex.net/v1/disk/resources/$suffix") {
            header(HttpHeaders.Authorization, "OAuth ${userToken.asString()}")
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
            user: User,
            secureTokenManager: SecureTokenManager,
            properties: YandexDiskStorageProperties
        ): YandexDiskRemoteStorage {
            return YandexDiskRemoteStorage(
                client,
                secureTokenManager.getTokenByUser(user),
                properties,
                user
            ).checkConnectionAndFolder()
        }
    }
}