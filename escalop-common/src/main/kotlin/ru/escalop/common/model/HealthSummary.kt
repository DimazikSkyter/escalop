package ru.escalop.ru.escalop.common.model

import ru.escalop.ru.escalop.common.entity.SnapshotSource
import com.fasterxml.jackson.databind.ObjectMapper

data class HealthSummary(
    val filter: String?,
    val healthDataByAnalysisTypeAndYear: Map<AnalysisType, Map<Int, List<HealthDataResult>>>
) {
    companion object {
        fun fromParts(healthPart: List<StorageHealthPart>): HealthSummary {
            val grouped: Map<AnalysisType, Map<Int, List<HealthDataResult>>> =
                healthPart
                    // 1. Группируем по типу анализа
                    .groupBy { AnalysisType.valueOf(it.analysisType) }
                    // 2. Для каждой группы по типу — группируем по году
                    .mapValues { (_, partsByType) ->
                        partsByType
                            .groupBy { it.year }
                            // 3. Для каждой группы по году склеиваем все metrics в один список
                            .mapValues { (_, partsByYear) ->
                                partsByYear.flatMap { it.metrics }
                            }
                    }

            return HealthSummary(
                filter = null,
                healthDataByAnalysisTypeAndYear = grouped
            )
        }
    }
}

data class StorageHealthPart(
    val analysisType: String,
    val year: Int,
    val index: Int,
    val metrics: List<HealthDataResult>
) {
    fun saveAsJson(): ByteArray {
        return OBJECT_MAPPER.writeValueAsBytes(this)
    }

    companion object {
        private val OBJECT_MAPPER = ObjectMapper()

        fun fromHealthDataResult(results: List<HealthDataResult>,
                                 analysisType: String,
                                 snapshotSource: SnapshotSource): StorageHealthPart {
            return StorageHealthPart(
                analysisType,
                snapshotSource.year,
                snapshotSource.index,
                results
            )
        }

        fun fromString(data: String): StorageHealthPart {
            return OBJECT_MAPPER.readValue(data, StorageHealthPart::class.java)
        }
    }
}