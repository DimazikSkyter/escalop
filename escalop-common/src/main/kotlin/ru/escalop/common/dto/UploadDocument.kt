package ru.escalop.ru.escalop.common.dto

import ru.escalop.ru.escalop.common.model.AnalysisType
import ru.escalop.ru.escalop.common.model.HealthDataResult
import ru.escalop.ru.escalop.common.model.HealthSummary

class UploadDocumentRequest {
}

data class UploadDocumentResponse (
    val statusCode: Int,
    val message: String?,
    val error: String?,
)
