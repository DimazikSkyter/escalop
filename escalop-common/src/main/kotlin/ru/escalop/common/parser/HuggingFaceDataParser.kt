package ru.escalop.common.parser

import ru.escalop.common.huggingface.FileMetricExtractorClient
import ru.escalop.common.model.HealthDataResult
import java.time.LocalDate


class HuggingFaceDataParser(
    val fileMetricExtractorClient: FileMetricExtractorClient,
    val healthDataValidator: HealthDataValidator
) : HealthDataParser {
    override suspend fun parse(name: String, date: LocalDate, fileBytes: ByteArray): HealthDataResult {
        val response: HealthDataResult = fileMetricExtractorClient.parseHealthData(name, date, fileBytes)
        healthDataValidator.validate(response)
        return response
    }
}