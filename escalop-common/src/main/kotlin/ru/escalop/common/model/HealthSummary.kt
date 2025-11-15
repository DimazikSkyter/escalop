package ru.escalop.ru.escalop.common.model

data class HealthSummary(
    val filter: String?,
    val healthDataByAnalysisTypeAndYear: Map<AnalysisType, Map<Int, HealthDataResult>>
)

data class StorageHealthPart(
    val fileType: String,
    val year: Int,
    val index: Int,
    val metrics: MutableList<HealthDataResult>
)