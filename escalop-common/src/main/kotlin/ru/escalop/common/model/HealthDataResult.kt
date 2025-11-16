package ru.escalop.ru.escalop.common.model

import java.time.LocalDate

data class Metric(
    val name: String,
    val value: Double,
    val minReferenceValue: Double?,
    val maxReferenceValue: Double?,
)

enum class AnalysisType {
    BLOOD_GENERAL,
    BLOOD_CHEMISTIC,
    ULTRASOUND,
    ANOTHER
}

data class HealthDataResult(
    var fileName: String,
    val date: LocalDate?,
    val analysisType: AnalysisType = AnalysisType.ANOTHER,
    val metrics: List<Metric>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HealthDataResult

        if (fileName != other.fileName) return false
        if (date != other.date) return false
        if (analysisType != other.analysisType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + analysisType.hashCode()
        return result
    }
}
