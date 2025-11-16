package ru.escalop.app

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.escalop.ru.escalop.common.dto.GetDataResponse
import ru.escalop.ru.escalop.common.dto.HealthDataResultDto
import ru.escalop.ru.escalop.common.dto.MetricDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

fun main(args: Array<String>): Unit = EngineMain.main(args)

//@Suppress("unused")
//fun Application.module() {
//    install(CallLogging)
//    install(Compression) { gzip() }
//    install(CORS) { anyHost(); allowNonSimpleContentTypes = true }
//    install(ContentNegotiation) { json(Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }) }
//
//    routing {
//        staticResources("/", "static") { default("index.html") }
//        route("/api") {
//            get("/series") {
//                val points = call.request.queryParameters["n"]?.toIntOrNull() ?: 50
//                val series = call.request.queryParameters.getAll("series")?.mapNotNull { it.toIntOrNull() } ?: listOf(1, 2)
//                val payload = ChartsPayload(
//                    labels = (1..points).map { "t$it" },
//                    datasets = series.map { idx ->
//                        Dataset("Series $idx", List(points) { Random(idx).nextInt(0, 100) + it % 7 })
//                    }
//                )
//                call.respond(payload)
//            }
//        }
//    }
//}

@Suppress("unused")
fun Application.module() {
    install(CallLogging)
    install(Compression) { gzip() }
    install(CORS) { anyHost(); allowNonSimpleContentTypes = true }
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        )
    }

    routing {
        // Отдаём статику (index.html, app.js, styles.css)
        staticResources("/", "static") { default("index.html") }

        route("/api") {
            // Новый эндпоинт: отдать моковый GetDataResponse
            get("/data") {
                val payload = generateMockGetDataResponse()
                call.respond(payload)
            }
        }
    }
}

private val bloodGeneralMetricNames = listOf(
    "Гемоглобин",
    "Лейкоциты",
    "Эритроциты",
    "Тромбоциты",
    "СОЭ"
)

private val bloodChemisticMetricNames = listOf(
    "Глюкоза",
    "Холестерин",
    "АЛТ",
    "АСТ",
    "Креатинин",
)

private val ultrasoundMetricNames = listOf(
    "Объём щитовидной железы",
    "Объём правой доли",
    "Объём левой доли",
    "Толщина перешейка",
    "Количество узлов"
)
private fun generateMockGetDataResponse(): GetDataResponse {
    val results = mutableListOf<HealthDataResultDto>()

    val baseYear = 2020
    val yearsCount = 5
    val random = Random(42)

    var docCounter = 1

    // Сколько анализов крови в каждом году (пример с «рваными» годами)
    // BLOOD_GENERAL
    val generalDocsPerYear = mapOf(
        2020 to 3,  // в этом году 3 документа
        2021 to 0,  // в этом году нет общих анализов
        2022 to 5,
        2023 to 2,
        2024 to 1
    )

    // BLOOD_CHEMISTIC
    val chemDocsPerYear = mapOf(
        2020 to 2,
        2021 to 1,
        2022 to 0,  // в этом году нет «химии»
        2023 to 3,
        2024 to 2
    )

    for (year in baseYear until baseYear + yearsCount) {
        // ---------- BLOOD_GENERAL ----------
        val generalCount = generalDocsPerYear[year] ?: 0
        if (generalCount > 0) {
            val months = (1..12).shuffled(random).take(generalCount)
            months.forEachIndexed { idx, month ->
                results += createMockDoc(
                    fileName = "blood_general_${year}_${idx}.pdf",
                    year = year,
                    month = month,
                    analysisType = "BLOOD_GENERAL",
                    metricNames = bloodGeneralMetricNames,
                    docIndex = docCounter++,
                    random = random
                )
            }
        }

        // ---------- BLOOD_CHEMISTIC ----------
        val chemCount = chemDocsPerYear[year] ?: 0
        if (chemCount > 0) {
            val months = (1..12).shuffled(random).take(chemCount)
            months.forEachIndexed { idx, month ->
                results += createMockDoc(
                    fileName = "blood_chemistic_${year}_${idx}.pdf",
                    year = year,
                    month = month,
                    analysisType = "BLOOD_CHEMISTIC",
                    metricNames = bloodChemisticMetricNames,
                    docIndex = docCounter++,
                    random = random
                )
            }
        }

        // ---------- ULTRASOUND ----------
        // Ровно одно УЗИ в год, но месяц каждый год разный и детерминированный
        val ultrasoundMonth = ((year - baseYear) * 3 + 2) % 12 + 1
        results += createMockDoc(
            fileName = "ultrasound_thyroid_${year}.pdf",
            year = year,
            month = ultrasoundMonth,
            analysisType = "ULTRASOUND",
            metricNames = ultrasoundMetricNames,
            docIndex = docCounter++,
            random = random
        )
    }

    val sorted = results.sortedBy { it.date }

    return GetDataResponse(
        status = 200,
        message = "Mock data OK",
        error = null,
        results = sorted
    )
}

private fun createMockDoc(
    fileName: String,
    year: Int,
    month: Int,
    analysisType: String,
    metricNames: List<String>,
    docIndex: Int,
    random: Random
): HealthDataResultDto {
    val safeMonth = month.coerceIn(1, 12)
    val date = LocalDate.of(year, safeMonth, (docIndex % 28 + 1))

    val baseShift = (year - 2000) * 0.3 + docIndex * 0.1

    val metrics = metricNames.mapIndexed { idx, name ->
        val base = when (analysisType) {
            "BLOOD_GENERAL" -> 100.0 + idx * 5
            "BLOOD_CHEMISTIC" -> 3.5 + idx * 0.7
            "ULTRASOUND" -> 5.0 + idx * 1.2
            else -> 0.0
        }
        val noise = random.nextDouble(-2.0, 2.0)
        MetricDto(
            name = name,
            value = base + baseShift + noise
        )
    }

    return HealthDataResultDto(
        fileName = fileName,
        date = date.toString(),
        analysisType = analysisType,
        metrics = metrics
    )
}

//@Serializable data class ChartsPayload(val labels: List<String>, val datasets: List<Dataset>)
//@Serializable data class Dataset(val label: String, val data: List<Int>, val color: String? = null, val shape: String? = null)