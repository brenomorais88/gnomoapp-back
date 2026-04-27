plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "com.dailyback"
version = "0.1.0"

val ktorVersion = "3.1.2"
val koinVersion = "3.5.6"
val exposedVersion = "0.59.0"
val flywayVersion = "11.8.0"
val postgresqlVersion = "42.7.7"

application {
    mainClass.set("com.dailyback.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")

    implementation("com.auth0:java-jwt:4.5.0")
    implementation("org.springframework.security:spring-security-crypto:6.4.4")
    implementation("org.springframework:spring-jcl:6.2.6")

    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

kover {
    reports {
        filters {
            excludes {
                classes("com.dailyback.MainKt")
                classes("com.dailyback.app.bootstrap.*")
                classes("com.dailyback.app.di.*")
                classes("com.dailyback.app.startup.*")
                classes("com.dailyback.shared.infrastructure.database.DatabaseFactory")
                classes("com.dailyback.shared.infrastructure.database.tables.*")
                classes("com.dailyback.shared.infrastructure.database.seeds.*")
                classes("com.dailyback.shared.infrastructure.migration.*")
                classes("com.dailyback.features.accounts.infrastructure.*")
                classes("com.dailyback.features.accounts.api.*")
                classes("com.dailyback.features.accountoccurrences.infrastructure.*")
                classes("com.dailyback.features.accountoccurrences.api.*")
                classes("com.dailyback.features.users.infrastructure.*")
                classes("com.dailyback.features.users.api.*")
                classes("com.dailyback.features.families.infrastructure.*")
                classes("com.dailyback.features.families.api.*")
            }
        }
        verify {
            rule {
                minBound(70)
            }
        }
    }
}
