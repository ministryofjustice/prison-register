import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.1.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
  idea
}

springBoot {
  mainClass.value("uk.gov.justice.digital.hmpps.prisonregister.PrisonRegisterApplicationKt")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("com.google.guava:guava:33.4.8-jre")
  implementation("commons-validator:commons-validator:1.9.0")
  implementation("com.googlecode.libphonenumber:libphonenumber:9.0.6")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.4")
  implementation("com.jayway.jsonpath:json-path:2.9.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.16.0")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.4")
  implementation("org.springframework.data:spring-data-commons:3.5.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.8")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.8.8")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.32")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
  implementation("org.apache.commons:commons-csv:1.14.0")
  implementation("org.apache.commons:commons-compress:1.27.1") // Address CVE-2024-25710 and CVE-2024-26308 present in v1.24
  implementation("org.freemarker:freemarker:2.3.34")
  implementation("io.netty:netty-common:4.2.1.Final")

  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.28")
  testImplementation("org.wiremock:wiremock-standalone:3.13.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.21.0")
  testImplementation("org.testcontainers:localstack:1.21.0")
  testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.783")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
repositories {
  mavenCentral()
}

tasks {
  register<PortForwardRDSTask>("portForwardRDS") {
    namespacePrefix = "hmpps-registers"
  }

  register<PortForwardRedisTask>("portForwardRedis") {
    namespacePrefix = "hmpps-registers"
  }

  register<RevealSecretsTask>("revealSecrets") {
    namespacePrefix = "hmpps-registers"
  }

  withType<KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }
}
