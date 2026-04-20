package ru.escalop.app

import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import ru.escalop.app.service.HealthDataService
import ru.escalop.app.service.YandexOAuthService
import ru.escalop.app.service.YandexOAuthServiceSettings
import ru.escalop.common.dto.GetDataResponse
import ru.escalop.common.dto.HealthDataResultDto
import ru.escalop.common.dto.MetricDto
import ru.escalop.common.entity.UserEntity
import ru.escalop.common.model.Document
import ru.escalop.common.model.User
import ru.escalop.common.utils.codeChallengeS256
import ru.escalop.common.utils.generateRandomString
import java.io.ByteArrayOutputStream
import java.sql.SQLException
import java.time.LocalDate
import kotlin.random.Random

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Serializable
data class UserSession(val userId: Long, val login: String)

@Serializable
data class OAuthSession(val state: String, val codeVerifier: String)

val logger = LoggerFactory.getLogger("MAIN")

@Suppress("unused")
fun Application.module() {
    logger.info("START MODULE")
    install(CallLogging)
    install(Compression) { gzip() }
    install(CORS) { anyHost(); allowNonSimpleContentTypes = true }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        })
    }

    install(Koin) {
        slf4jLogger()
        modules(
            module { single<ApplicationEnvironment> { environment } }, // чтобы доставать конфиг
            jpaModule,
            appModule // тут твои сервисы / репозитории
        )
    }

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.httpOnly = true
        }
        cookie<OAuthSession>("oauth_tokens") {
            cookie.path = "/"
            cookie.httpOnly = true
        }
    }

    val dataSource = get<HikariDataSource>()
    val healthDataService = get<HealthDataService>()

    routing {

        // Статика: /static/styles.css, /static/app.js, /static/index.html и т.д.
        staticResources("/static", "static")

        // --- Welcome + Login страница ---
        get("/") {
            val loginError = call.request.queryParameters["error"]
            val html = loadResourceText("static/login.html").replace(
                "{{error}}",
                loginError?.let { "Неверный логин или пароль" } ?: "")
            call.respondText(html, ContentType.Text.Html)
        }

        // загрузка токена
        get("/oauth/yandex/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            val oauthSession = call.sessions.get<OAuthSession>()

            if (code == null || state == null || oauthSession == null) {
                call.respondText("OAuth data is missing", status = HttpStatusCode.BadRequest)
                return@get
            }

            if (state != oauthSession.state) {
                call.respondText("Invalid state", status = HttpStatusCode.BadRequest)
                return@get
            }

            val oauthService = YandexOAuthService(
                clientId = environment.config.property("yandex.oauth.clientId").getString(),
                redirectUri = environment.config.property("yandex.oauth.redirectUri").getString(),
                httpClient = get(),
                settings = YandexOAuthServiceSettings(
                    "https://oauth.yandex.ru/authorize",
                    "https://oauth.yandex.com/token"
                )
            )

            val tokens = oauthService.exchangeCode(
                code = code,
                codeVerifier = oauthSession.codeVerifier
            )

            // TODO: сохранить tokens.accessToken / tokens.refreshToken
            // например в БД или в отдельное хранилище

            call.sessions.clear<OAuthSession>()

            call.respondText("Авторизация завершена. Можно вернуться в приложение.")
        }

        get("/oauth/yandex/start") {
            val state = generateRandomString()
            val codeVerifier = generateRandomString()
            val codeChallenge = codeChallengeS256(codeVerifier)

            call.sessions.set(
                OAuthSession(
                    state = state,
                    codeVerifier = codeVerifier
                )
            )

            val oauthService = YandexOAuthService(
                clientId = environment.config.property("yandex.oauth.clientId").getString(),
                redirectUri = environment.config.property("yandex.oauth.redirectUri").getString(),
                httpClient = get(),
                settings = YandexOAuthServiceSettings(
                    "https://oauth.yandex.ru/authorize",
                    "https://oauth.yandex.com/token"
                )
            )

            val authorizeUrl = oauthService.buildAuthorizeUrl(
                state = state,
                codeChallenge = codeChallenge
            )

            call.respondRedirect(authorizeUrl)
        }

        // --- Обработка логина ---
        post("/login") {
            val params = call.receiveParameters()
            val login = params["login"]?.trim().orEmpty()
            val password = params["password"]?.trim().orEmpty()

            val user = findUserByCredentials(dataSource, login, password)

            if (user == null || user.id == null) {
                call.respondRedirect("/?error=1")
                return@post
            }

            call.sessions.set(UserSession(userId = user.id!!, login = user.login))
            call.respondRedirect("/visualization")
        }
        get("/visualization") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/")
                return@get
            }
            val html = loadResourceText("static/visualization.html")
            call.respondText(html, ContentType.Text.Html)
        }

        get("/visualization2") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respondRedirect("/")
                return@get
            }
            val html = loadResourceText("static/index.html")
            call.respondText(html, ContentType.Text.Html)
        }

        // Можно добавить logout, если нужно
        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/")
        }

        // --- API c моковыми данными (оставляем как есть) ---
        route("/api") {
            get("/data") {
                val payload = generateMockGetDataResponse()
                call.respond(payload)
            }

            get("/uploads") {
                logger.info("Start uploads history")
                val session = call.sessions.get<UserSession>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                logger.info("Go to service")
                val history = healthDataService.getUploadHistory(User(session.login))
                call.respond(history)
            }

            post("/upload") {
                val session = call.sessions.get<UserSession>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, "Unauthorized")

                val multipart = call.receiveMultipart()

                var fileName: String? = null
                val baos = ByteArrayOutputStream()

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            // Берём первый файл
                            if (fileName == null) {
                                fileName = part.originalFileName ?: "upload.bin"
                                val bytes = part.streamProvider().readBytes()
                                baos.reset()
                                baos.write(bytes)
                            }
                        }

                        else -> {}
                    }
                    part.dispose()
                }

                if (fileName == null || baos.size() == 0) {
                    return@post call.respond(HttpStatusCode.BadRequest, "File is required")
                }

                val document = Document(
                    name = fileName!!,
                    body = baos.toByteArray()
                )

                val result = healthDataService.uploadNewDocument(User(session.login), document)
                call.respond(result)
            }
        }
    }
}

private val bloodGeneralMetricNames = listOf(
    "Гемоглобин", "Лейкоциты", "Эритроциты", "Тромбоциты", "СОЭ"
)

private val bloodChemisticMetricNames = listOf(
    "Глюкоза",
    "Холестерин",
    "АЛТ",
    "АСТ",
    "Креатинин",
)

private val ultrasoundMetricNames = listOf(
    "Объём щитовидной железы", "Объём правой доли", "Объём левой доли", "Толщина перешейка", "Количество узлов"
)

private fun Application.loadResourceText(path: String): String {
    val url = environment.classLoader.getResource(path) ?: error("Resource not found: $path")
    return url.readText()
}

private fun findUserByCredentials(dataSource: HikariDataSource, login: String, password: String): UserEntity? {
    if (login.isBlank() || password.isBlank()) return null

    val sql = """
        SELECT id, internal_key, login, password
        FROM users
        WHERE login = ? AND password = ?
        LIMIT 1
    """.trimIndent()

    try {
        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, login)
                ps.setString(2, password)

                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        return UserEntity(
                            id = rs.getLong("id"),
                            internalKey = rs.getString("internal_key"),
                            login = rs.getString("login"),
                            password = rs.getString("password")
                        )
                    }
                }
            }
        }
    } catch (e: SQLException) {
        // можно залогировать, но падать из-за авторизации не хочется
        e.printStackTrace()
    }

    return null
}


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
        2022 to 5, 2023 to 2, 2024 to 1
    )

    // BLOOD_CHEMISTIC
    val chemDocsPerYear = mapOf(
        2020 to 2, 2021 to 1, 2022 to 0,  // в этом году нет «химии»
        2023 to 3, 2024 to 2
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
        status = 200, message = "Mock data OK", error = null, results = sorted
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
            name = name, value = base + baseShift + noise
        )
    }

    return HealthDataResultDto(
        fileName = fileName, date = date.toString(), analysisType = analysisType, metrics = metrics
    )
}

//@Serializable data class ChartsPayload(val labels: List<String>, val datasets: List<Dataset>)
//@Serializable data class Dataset(val label: String, val data: List<Int>, val color: String? = null, val shape: String? = null)