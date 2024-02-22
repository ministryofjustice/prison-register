plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.2"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
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
  implementation("commons-validator:commons-validator:1.7")
  implementation("com.googlecode.libphonenumber:libphonenumber:8.13.12")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")
  implementation("com.jayway.jsonpath:json-path:2.9.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.30.0")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
  implementation("org.springframework.data:spring-data-commons:3.2.3")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.2.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
  implementation("org.springdoc:springdoc-openapi-starter-common:2.2.0")
  implementation("io.swagger:swagger-annotations:1.6.11")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("net.javacrumbs.shedlock:shedlock-spring:5.7.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.7.0")
  implementation("org.apache.commons:commons-csv:1.10.0")
  implementation("org.freemarker:freemarker:2.3.32")

  runtimeOnly("org.postgresql:postgresql:42.6.0")
  runtimeOnly("org.flywaydb:flyway-core")

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.16")
  testImplementation("org.wiremock:wiremock-standalone:3.4.1")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.testcontainers:localstack:1.18.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.testcontainers:postgresql:1.19.5")
  testImplementation("org.testcontainers:localstack:1.19.5")
  testImplementation("com.amazonaws:aws-java-sdk-s3:1.12.662")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
repositories {
  mavenCentral()
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
