plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "ru.escalop"
val ktorVersion: String by project
val koinVersion: String by project

repositories {
    mavenCentral()
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    implementation(project(":escalop-common"))

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.5")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.hibernate.orm:hibernate-core:6.5.2.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Koin
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
