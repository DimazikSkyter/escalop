package ru.escalop.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Metric(
    val name: String,
    val value: Double,
    val minReferenceValue: Double?,
    val maxReferenceValue: Double?,
)

enum class AnalysisType {
    BLOOD_GENERAL("Общий анализ крови"),
    BLOOD_CHEMISTIC("Биохимический анализ крови"),
    ULTRASOUND("Ультразвуковое обследование"),
    ANOTHER("Любой");

    val rusStr: String

    constructor( rusStr: String){
        this.rusStr = rusStr
    }

    companion object {
        fun getTypeOrDefault(type: String?): AnalysisType {
            return type?.let {
                try {
                    enumValueOf<AnalysisType>(type)
                } catch (e: IllegalArgumentException) {
                    ANOTHER
                }
            } ?: ANOTHER
        }
    }
}

data class HealthDataResult(
    var fileName: String,
    val date: LocalDate?,
    val analysisType: AnalysisType = AnalysisType.ANOTHER,
    val metrics: List<Metric>,
) {

    companion object {
        fun fromAnalysisDocument(fileName: String,
                                 date: LocalDate?,
                                 analysisDocument: Map<String, Map<String, JsonElement>>): HealthDataResult {
            val entry = analysisDocument.entries.first()
            val dateStr = entry.value["Дата анализа"].toString().replace("\"", "")
            val date = if (dateStr.length > 8) {
                LocalDate.parse(dateStr,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            } else {
                LocalDate.parse(entry.value["Дата анализа"].toString().replace("\"", ""),
                    DateTimeFormatter.ofPattern("dd.MM.yy"))
            }
            return HealthDataResult(fileName,
                date,
                AnalysisType.getTypeOrDefault(entry.key),
                analysisDocument.entries.first().value.filter { !it.key.contains("Дата") }.map { Metric(
                    it.key,
                    it.value.jsonPrimitive.double,
                    Double.MIN_VALUE,
                    Double.MAX_VALUE) }
                )
        }
    }

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
