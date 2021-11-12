plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.13-beta-2"
  kotlin("plugin.spring") version "1.5.31"
  kotlin("plugin.jpa") version "1.5.31"
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

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("org.flywaydb:flyway-core:8.0.0")
  runtimeOnly("org.postgresql:postgresql:42.2.24")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.google.code.gson:gson:2.8.8")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.5")

  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.5.11")
  implementation("org.springdoc:springdoc-openapi-ui:1.5.11")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.11")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
