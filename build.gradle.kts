import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  id("org.jetbrains.kotlinx.kover") version "0.9.4"
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
  implementation("commons-validator:commons-validator:1.10.0")
  implementation("com.googlecode.libphonenumber:libphonenumber:9.0.13")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")
  implementation("com.jayway.jsonpath:json-path:2.9.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.19.0")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
  implementation("org.springframework.data:spring-data-commons:3.5.7")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.13")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.8.13")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.36")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("net.javacrumbs.shedlock:shedlock-spring:7.5.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.5.0")
  implementation("org.apache.commons:commons-csv:1.14.1")
  implementation("org.apache.commons:commons-compress:1.28.0")
  implementation("org.freemarker:freemarker:2.3.34")
  implementation("io.netty:netty-common:4.2.3.Final")

  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.33")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.jsonwebtoken:jjwt:0.13.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.791")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xwhen-guards", "-Xannotation-default-target=param-property")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
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
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }

  test {
    maxHeapSize = "2048m"
  }
}
