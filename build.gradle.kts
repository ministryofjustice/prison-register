import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.1"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  id("org.jetbrains.kotlinx.kover") version "0.8.2"
  idea
}

springBoot {
  mainClass.value("uk.gov.justice.digital.hmpps.prisonregister.PrisonRegisterApplicationKt")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("com.google.guava:guava:32.1.2-jre")
  implementation("commons-validator:commons-validator:1.9.0")
  implementation("com.googlecode.libphonenumber:libphonenumber:8.13.41")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")
  implementation("com.jayway.jsonpath:json-path:2.9.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.33.4")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
  implementation("org.springframework.data:spring-data-commons:3.3.2")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.6.0")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.22")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("net.javacrumbs.shedlock:shedlock-spring:5.13.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.13.0")
  implementation("org.apache.commons:commons-csv:1.11.0")
  implementation("org.freemarker:freemarker:2.3.33")

  runtimeOnly("org.postgresql:postgresql:42.7.3")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.22")
  testImplementation("org.wiremock:wiremock-standalone:3.8.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.testcontainers:localstack:1.19.8")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.19.8")
  testImplementation("org.testcontainers:localstack:1.19.8")
  testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.761")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
repositories {
  mavenCentral()
}

tasks {
  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
