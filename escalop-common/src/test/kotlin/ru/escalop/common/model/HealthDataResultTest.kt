package ru.escalop.common.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test

class HealthDataResultTest {

    @Test
    fun testCreateFile() {

        val json = """
{
  "Биохимический анализ крови": {
    "Дата анализа": "25.09.2025",
    "Аланинаминотрансфераза (АЛТ)": 98.35,
    "Аспартатаминотрансфераза (АСТ)": 62.69,
    "Глюкоза": 5.37,
    "Креатинин": 142.1,
    "Мочевина": 17.05,
    "Мочевая кислота": 503.79,
    "Общий белок": 71.9,
    "Холестерин общий": 2.59,
    "С-реактивный белок": 131.25
  }
}
""".trimIndent()

        val parsed: Map<String, Map<String, JsonElement>> =
            Json.decodeFromString(json)

        print(parsed)

    }
}