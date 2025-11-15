package ru.escalop.ru.escalop.common.huggingface

import ru.escalop.ru.escalop.common.model.HealthDataResult

interface FileMetricExtractorClient {

    suspend fun parseHealthData(
        fileName: String,
        fileBytes: ByteArray,
    ): HealthDataResult
}