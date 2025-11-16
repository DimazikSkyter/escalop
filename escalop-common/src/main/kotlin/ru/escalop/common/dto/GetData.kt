package ru.escalop.ru.escalop.common.dto

import kotlinx.serialization.Serializable
import ru.escalop.ru.escalop.common.model.*
import java.time.format.DateTimeFormatter

//todo перенести в app
@Serializable
class GetDataRequest {
}

@Serializable
data class MetricDto(
    val name: String,
    val value: Double,
) {
    companion object {
        fun fromMetrics(metric: Metric): MetricDto {
            return MetricDto(
                metric.name,
                metric.value
            )
        }
    }
}

@Serializable
data class HealthDataResultDto(
    var fileName: String,
    val date: String?,
    val analysisType: String,
    val metrics: List<MetricDto>,
) {
    companion object {
        fun fromHealthDataResult(healthDataResult: HealthDataResult): HealthDataResultDto {
            return HealthDataResultDto(
                healthDataResult.fileName,
                healthDataResult.date?.toString(),
                healthDataResult.analysisType.name,
                healthDataResult.metrics.map { MetricDto.fromMetrics(it) }
            )
        }

        fun fromStorageHealthPart(storageHealthPart: StorageHealthPart): List<HealthDataResultDto> {
            return storageHealthPart.metrics.map { fromHealthDataResult(it) }
        }
    }
}

@Serializable
data class GetDataResponse (
    val status: Int,
    val message: String,
    val error: String?,
    val results: List<HealthDataResultDto>
) {
    companion object {
        fun fromHealthDataResult(status: Int,
                                 message: String,
                                 healthParts: List<StorageHealthPart>
        ): GetDataResponse {
            return GetDataResponse(
                status,
                message,
                null,
                healthParts.flatMap{ HealthDataResultDto.fromStorageHealthPart(it) }
            )
        }
    }
}
