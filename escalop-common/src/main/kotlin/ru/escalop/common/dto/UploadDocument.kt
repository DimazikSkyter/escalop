package ru.escalop.common.dto

import kotlinx.serialization.Serializable
import ru.escalop.common.model.AnalysisType
import ru.escalop.common.model.HealthDataResult
import ru.escalop.common.model.HealthSummary

class UploadDocumentRequest {
}


@Serializable
data class UploadDocumentResponse (
    val statusCode: Int,
    val message: String?,
    val error: String?,
)
