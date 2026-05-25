package ru.escalop.common.huggingface

import ru.escalop.common.model.HealthDataResult
import java.time.LocalDate

interface FileMetricExtractorClient {

    suspend fun parseHealthData(
        fileName: String,
        date: LocalDate,
        fileBytes: ByteArray,
    ): HealthDataResult
}