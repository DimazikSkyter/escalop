package ru.escalop.common.dto

import kotlinx.serialization.Serializable


@Serializable
data class UploadHistoryItem (
    val fileName: String,
    val format: String,
    val date: String?,
    val analysisType: String,
)