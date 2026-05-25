package ru.escalop.common.parser

import ru.escalop.common.model.AnalysisType
import ru.escalop.common.model.HealthDataResult
import ru.escalop.common.model.Metric
import java.time.LocalDate

interface HealthDataParser {

    suspend fun parse(name: String, date: LocalDate, fileBytes: ByteArray): HealthDataResult

    companion object {
        val NONE: HealthDataParser = object : HealthDataParser {
            override suspend fun parse(name: String, date: LocalDate, fileBytes: ByteArray): HealthDataResult {
                TODO("Not yet implemented")
            }
        }
    }
}

class MockHealthDataParser (
    private val fakeMetrics: List<Metric>
) : HealthDataParser {
    override suspend fun parse(name: String,
                               date: LocalDate,
                               fileBytes: ByteArray): HealthDataResult {
        return HealthDataResult(
            name,
            date,
            AnalysisType.ANOTHER,
            fakeMetrics
        )
    }
}