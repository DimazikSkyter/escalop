rootProject.name = "escalop"

include("escalop-app")
include("escalop-common")
//include("escalop-ktor-app")


pluginManagement {
    val kotlinVersion: String by settings
//    val openapiVersion: String by settings
//    val kotestVersion: String by settings
    val ktorVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion apply false
        kotlin("multiplatform") version kotlinVersion
//        kotlin("plugin.serialization") version kotlinVersion apply false
//        kotlin("plugin.jpa") version kotlinVersion apply false
//        id("io.kotest.multiplatform") version kotestVersion apply false
        id("io.ktor.plugin") version ktorVersion apply false

//        id("org.openapi.generator") version openapiVersion apply false

    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // оставь так
        repositories {
            mavenCentral()
            // google(), maven("...") — по необходимости
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
