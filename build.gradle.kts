import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starter Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Если нужно именно javax.servlet (не рекомендуется для новых проектов)
    implementation("javax.servlet:javax.servlet-api:4.0.1") {
        because("Legacy javax.servlet support")
    }
    // Apache HttpClient
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Тестирование
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    // TestContainers BOM (Bill of Materials) для управления версиями
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.3"))

    // Основные модули TestContainers
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")

    // Специфичные модули (для ClickHouse)
    testImplementation("org.testcontainers:clickhouse")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Конфигурация для сборки исполняемого JAR
tasks.bootJar {
    archiveFileName.set("https-balancer.jar")
    launchScript()
}

// Настройка Spring Boot для использования SSL
tasks.register("configureSsl") {
    doLast {
        val keystore = File("src/main/resources/keystore.p12")
        if (!keystore.exists()) {
            throw GradleException("SSL keystore not found at ${keystore.path}. Please generate it first.")
        }
    }
}

tasks.bootRun {
    dependsOn("configureSsl")
    systemProperties = mapOf(
        "server.port" to "8443",
        "server.ssl.enabled" to "true",
        "server.ssl.key-store" to "classpath:keystore.p12",
        "server.ssl.key-store-password" to "changeit",
        "server.ssl.key-store-type" to "PKCS12"
    )
}