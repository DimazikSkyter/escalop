package ru.escalop.ru.escalop.common.model

import java.time.LocalDate

data class Filter(
    val fileName: String?,
    val analysisType: AnalysisType?,
    val date: LocalDate?,
    val year: Int?,
)
