import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.3.1"
  kotlin("plugin.spring") version "2.3.21"
  kotlin("plugin.jpa") version "2.3.21"
  id("org.jetbrains.kotlinx.kover") version "0.9.8"
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
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.5.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.3.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.28.1")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

  implementation("com.jayway.jsonpath:json-path:3.0.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")

  implementation("net.javacrumbs.shedlock:shedlock-spring:7.5.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.5.0")
  implementation("org.apache.commons:commons-csv:1.14.1")
  implementation("org.apache.commons:commons-compress:1.28.0")
  implementation("org.freemarker:freemarker:2.3.34")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  implementation("com.zaxxer:HikariCP:7.0.2")
  runtimeOnly("org.postgresql:postgresql:42.7.11")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.5.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")

  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.42") {
    exclude(group = "io.swagger.core.v3")
  }

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
  testImplementation("org.testcontainers:testcontainers-localstack:2.0.5")
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
