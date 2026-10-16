package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

class ReferenceDataControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class Security {
    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/suggestions/counties",
        "/reference-data/suggestions/towns",
        "/reference-data/areas",
        "/reference-data/regions",
        "/reference-data/payroll-regions",
        "/reference-data/local-authorities",
        "/reference-data/areas?region=SW",
        "/reference-data/subareas?area=32",
      ],
    )
    fun `requires a valid authentication token`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/suggestions/counties",
        "/reference-data/suggestions/towns",
        "/reference-data/areas",
        "/reference-data/regions",
        "/reference-data/payroll-regions",
        "/reference-data/local-authorities",
        "/reference-data/areas?region=SW",
        "/reference-data/subareas?area=32",
      ],
    )
    fun `requires correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/suggestions/counties",
        "/reference-data/suggestions/towns",
        "/reference-data/areas",
        "/reference-data/regions",
        "/reference-data/payroll-regions",
        "/reference-data/local-authorities",
        "/reference-data/areas?region=SW",
        "/reference-data/subareas?area=32",
      ],
    )
    fun `allowed with correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class GetReferenceData {
    @ParameterizedTest
    @CsvSource(
      "areas, 104, CI, Channel Isles, Avon and Somerset, Yorkshire & Humberside",
      "regions, 16, SW, South West, Contracted, Young People",
      "payroll-regions, 12, WAL, Wales, Contracted, Young People",
      "local-authorities, 393, 00CG, Sheffield City Council, Adur, York",
    )
    fun `will return all reference data ordered by description`(
      path: String,
      size: Int,
      code: String,
      description: String,
      firstDescription: String,
      lastDescription: String,
    ) {
      val data = getReferenceData("/reference-data/$path?active-only=false")

      assertThat(data).hasSize(size)
      assertThat(data).contains(CodeDescription(code, description))
      assertThat(data.first().description).isEqualTo(firstDescription)
      assertThat(data.last().description).isEqualTo(lastDescription)
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/areas",
        "/reference-data/regions",
        "/reference-data/payroll-regions",
        "/reference-data/local-authorities",
        "/reference-data/areas?region=SW",
        "/reference-data/subareas?area=32",
      ],
    )
    fun `will return only code and description`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0]").value<Map<String, Any>> { assertThat(it).containsOnlyKeys("code", "description") }
    }

    @Test
    fun `will return only areas belonging to the requested region including inactive areas`() {
      val areas = getReferenceData("/reference-data/areas?region=SW&active-only=false")

      assertThat(areas).containsExactlyInAnyOrder(
        CodeDescription("11", "Avon & Somerset"),
        CodeDescription("AVON_SOM", "Avon and Somerset"),
        CodeDescription("21", "Devon & Cornwall"),
        CodeDescription("DEVON_CORN", "Devon and Cornwall"),
        CodeDescription("22", "Dorset"),
        CodeDescription("DORSET", "Dorset"),
        CodeDescription("25", "Gloucestershire"),
        CodeDescription("GLOUS", "Gloucestershire"),
        CodeDescription("SWEST", "South West"),
        CodeDescription("50", "Wiltshire"),
        CodeDescription("WILTSHIRE", "Wiltshire"),
      )
      assertThat(areas.first()).isEqualTo(CodeDescription("AVON_SOM", "Avon and Somerset"))
      assertThat(areas.last().description).isEqualTo("Wiltshire")
    }

    @Test
    fun `will return only subareas belonging to the requested area`() {
      assertThat(getReferenceData("/reference-data/subareas?area=32")).containsExactly(
        CodeDescription("CITY", "City"),
        CodeDescription("COUNT", "County"),
      )
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/areas?region=UNKNOWN",
        "/reference-data/areas?region=HS",
        "/reference-data/subareas?area=UNKNOWN",
        "/reference-data/subareas?area=CI",
      ],
    )
    fun `will return an empty list when the parent has no children or does not exist`(uri: String) {
      assertThat(getReferenceData(uri)).isEmpty()
    }

    @Test
    fun `subareas require an area parameter`() {
      webTestClient.get()
        .uri("/reference-data/subareas")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @Sql(
    statements = [
      "INSERT INTO region (code, description, active) VALUES ('TEST_ACTIVE', 'Test active', true), ('TEST_INACT', 'Test inactive', false)",
      "INSERT INTO area (code, description, parent, prison_area, active) VALUES ('TEST_ACTIVE', 'Test active', 'SW', false, true), ('TEST_INACT', 'Test inactive', 'SW', false, false)",
      "INSERT INTO payroll_region (code, description, active) VALUES ('TEST_ACTIVE', 'Test active', true), ('TEST_INACT', 'Test inactive', false)",
      "INSERT INTO local_authority (code, description, active) VALUES ('TEST_ACTIVE', 'Test active', true), ('TEST_INACT', 'Test inactive', false)",
      "INSERT INTO subarea (code, description, parent, active) VALUES ('TEST_ACTIVE', 'Test active', '32', true), ('TEST_INACT', 'Test inactive', '32', false)",
    ],
  )
  @Sql(
    statements = [
      "DELETE FROM subarea WHERE code IN ('TEST_ACTIVE', 'TEST_INACT')",
      "DELETE FROM local_authority WHERE code IN ('TEST_ACTIVE', 'TEST_INACT')",
      "DELETE FROM payroll_region WHERE code IN ('TEST_ACTIVE', 'TEST_INACT')",
      "DELETE FROM area WHERE code IN ('TEST_ACTIVE', 'TEST_INACT')",
      "DELETE FROM region WHERE code IN ('TEST_ACTIVE', 'TEST_INACT')",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
  )
  inner class ActiveFilter {
    @ParameterizedTest
    @ValueSource(
      strings = [
        "/reference-data/areas",
        "/reference-data/regions",
        "/reference-data/payroll-regions",
        "/reference-data/local-authorities",
        "/reference-data/areas?region=SW",
        "/reference-data/subareas?area=32",
      ],
    )
    fun `will return only active data by default and include inactive data only when requested`(uri: String) {
      val separator = if (uri.contains("?")) "&" else "?"
      val active = CodeDescription("TEST_ACTIVE", "Test active")
      val inactive = CodeDescription("TEST_INACT", "Test inactive")

      val defaultData = getReferenceData(uri)
      val activeData = getReferenceData("$uri${separator}active-only=true")
      val allData = getReferenceData("$uri${separator}active-only=false")

      assertThat(defaultData).isEqualTo(activeData)
      assertThat(activeData).contains(active).doesNotContain(inactive)
      assertThat(allData).contains(active, inactive).containsAll(activeData)
      assertThat(allData.filter { it == active || it == inactive }).containsExactly(active, inactive)
    }
  }

  private fun getReferenceData(uri: String): List<CodeDescription> = webTestClient.get()
    .uri(uri)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyList(CodeDescription::class.java)
    .returnResult().responseBody!!

  @DisplayName("GET /reference-data/suggestions/counties")
  @Nested
  inner class GetCountySuggestions {
    @Test
    fun `will return all county suggestions`() {
      val counties: List<CodeDescription> = webTestClient.get()
        .uri("/reference-data/suggestions/counties")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(CodeDescription::class.java)
        .returnResult().responseBody!!

      assertThat(counties).hasSizeGreaterThan(10)
      assertThat(counties.map { it.description }).contains("South Yorkshire")
      assertThat(counties).contains(CodeDescription(code = "S.YORKSHIRE", description = "South Yorkshire"))
    }
  }

  @DisplayName("GET /reference-data/suggestions/towns")
  @Nested
  inner class GetTownSuggestions {
    @Test
    fun `will return all town suggestions`() {
      val towns: List<CodeDescription> = webTestClient.get()
        .uri("/reference-data/suggestions/towns")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__MAINTAIN__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyList(CodeDescription::class.java)
        .returnResult().responseBody!!

      assertThat(towns).hasSizeGreaterThan(100)
      assertThat(towns.map { it.description }).contains("Sheffield", "Chelmsford", "Colchester")
      assertThat(towns).contains(CodeDescription(code = "25343", description = "Sheffield"))
    }
  }
}
