package ru.escalop.app.huggingface

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.common.model.HealthDataResult

//class HuggingFaceClientImpl(
//    private val httpClient: HttpClient,
//    private val baseUrl: String,
//    private val apiToken: String,
//    /**
//     * Путь до эндпоинта, который принимает PDF и возвращает HealthDataResult.
//     * Можно переопределить снаружи, если у тебя другой путь.
//     */
//    private val endpointPath: String = "/health-metrics",
//): FileMetricExtractorClient {
//
//
//    /**
//     * Отправляет файл (pdf, картинка, doc/docx и т.д.) и возвращает HealthDataResult.
//     *
//     * @param fileName   имя файла (уйдёт в multipart filename)
//     * @param fileBytes  содержимое файла
//     */
//    override suspend fun parseHealthData(
//        fileName: String,
//        fileBytes: ByteArray,
//    ): HealthDataResult {
//        val contentType: ContentType = guessContentType(fileName)
//        val response = httpClient.post("$baseUrl$endpointPath") {
//            headers {
//                append(HttpHeaders.Authorization, "Bearer $apiToken")
//            }
//
//            setBody(
//                MultiPartFormDataContent(
//                    formData {
//                        append(
//                            key = "file",
//                            value = fileBytes,
//                            headers = Headers.build {
//                                append(
//                                    HttpHeaders.ContentDisposition,
//                                    ContentDisposition.File.withParameter(
//                                        ContentDisposition.Parameters.FileName,
//                                        "name",
//                                    ).toString()
//                                )
//                                append(HttpHeaders.ContentType, contentType.toString())
//                            }
//                        )
//                    }
//                )
//            )
//        }
//
//        // Ktor + Jackson конвертируют JSON-ответ в HealthDataResult
//        return response.body()
//    }
//
//    private fun guessContentType(fileName: String): ContentType {
//        val ext = fileName.substringAfterLast('.', "").lowercase()
//
//        return when (ext) {
//            "pdf" -> ContentType.Application.Pdf
//            "png" -> ContentType.Image.PNG
//            "jpg", "jpeg" -> ContentType.Image.JPEG
//            "gif" -> ContentType.Image.GIF
//            "doc" -> ContentType.parse("application/msword")
//            "docx" -> ContentType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
//            else -> throw RuntimeException("Unknown file type for file with name $fileName")
//        }
//    }
//
//    companion object {
//        fun default(
//            baseUrl: String,
//            apiToken: String,
//            endpointPath: String = "/health-metrics",
//        ): FileMetricExtractorClient {
//            val client = HttpClient(CIO) {
//                install(ContentNegotiation) {
//                    jackson {
//                        registerModule(JavaTimeModule())
//                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
//                    }
//                }
//            }
//
//            return HuggingFaceClientImpl(
//                httpClient = client,
//                baseUrl = baseUrl.trimEnd('/'),
//                apiToken = apiToken,
//                endpointPath = endpointPath,
//            )
//        }
//    }
//}
class HuggingFaceClientImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiToken: String,
    // путь по умолчанию меняем на /extract
    private val endpointPath: String = "/extract",
) : FileMetricExtractorClient {

    override suspend fun parseHealthData(
        fileName: String,
        fileBytes: ByteArray,
    ): HealthDataResult {
        val contentType: ContentType = guessContentType(fileName)

        val response = httpClient.post("$baseUrl$endpointPath") {
            // если токен не нужен — можно вообще удалить этот блок
            if (apiToken.isNotBlank()) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiToken")
                }
            }

            setBody(
                MultiPartFormDataContent(
                    formData {
                        // ИМЯ ПОЛЯ ДОЛЖНО БЫТЬ files
                        append(
                            key = "files",
                            value = fileBytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(
                                            ContentDisposition.Parameters.Name,
                                            "files",          // name="files"
                                        )
                                        .withParameter(
                                            ContentDisposition.Parameters.FileName,
                                            fileName,         // filename="тот_же_что_ты_передал"
                                        )
                                        .toString()
                                )
                                append(HttpHeaders.ContentType, contentType.toString())
                            }
                        )
                    }
                )
            )
        }

        return response.body()
    }

    private fun guessContentType(fileName: String): ContentType {
        val ext = fileName.substringAfterLast('.', "").lowercase()

        return when (ext) {
            "pdf" -> ContentType.Application.Pdf
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            "doc" -> ContentType.parse("application/msword")
            "docx" -> ContentType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            else -> throw RuntimeException("Unknown file type for file with name $fileName")
        }
    }
}
