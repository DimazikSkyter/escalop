plugins {
    kotlin("jvm")
    kotlin("plugin.jpa") version "1.9.23"
    kotlin("plugin.allopen") version "1.9.23"
    kotlin("plugin.serialization")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

group = "ru.escalop"
val ktorVersion: String by project

dependencies {

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion") // или okhttp/java
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Hibernate ORM (JPA implementation)
    implementation("org.hibernate.orm:hibernate-core:6.5.2.Final")

    // JPA API
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.3")

    // опционально:
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.5")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.13")
}

tasks.test {
    useJUnitPlatform()
}