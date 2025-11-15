package ru.escalop.ru.escalop.common.parser

import ru.escalop.ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.ru.escalop.common.model.HealthDataResult


class HuggingFaceDataParser(
    val fileMetricExtractorClient: FileMetricExtractorClient,
    val healthDataValidator: HealthDataValidator
) : HealthDataParser {
    override suspend fun parse(name: String, fileBytes: ByteArray): HealthDataResult {
        val response: HealthDataResult = fileMetricExtractorClient.parseHealthData(name, fileBytes)
        healthDataValidator.validate(response)
        return response
    }
}