plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.5-beta-2"
  kotlin("plugin.spring") version "1.6.21"
  kotlin("plugin.jpa") version "1.6.21"
  idea
}

configurations {
  implementation {
    exclude(group = "tomcat-jdbc")
    exclude(module = "spring-boot-graceful-shutdown")
  }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.3")

  runtimeOnly("com.h2database:h2:2.1.212")
  runtimeOnly("org.flywaydb:flyway-core:8.5.8")
  runtimeOnly("org.postgresql:postgresql:42.3.4")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.google.code.gson:gson:2.9.0")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.7")
  implementation("org.springdoc:springdoc-openapi-ui:1.6.7")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.7")

  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.34.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
  testImplementation("org.mockito:mockito-inline:4.5.0")
  testImplementation("org.springframework.security:spring-security-test")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
