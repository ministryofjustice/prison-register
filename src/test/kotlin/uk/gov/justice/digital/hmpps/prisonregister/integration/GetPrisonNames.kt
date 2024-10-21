package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto

class GetPrisonNames : IntegrationTest() {
  @Test
  fun `should return prison names and ids by name order`() {
    // Given
    val endPont = "/prisons/names"

    // When
    val responseSpec = webTestClient.get().uri(endPont)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("AKI")
      Assertions.assertThat(prisonName).isEqualTo("Acklington (HMP)")
    }
    with(prisonNames.last()) {
      Assertions.assertThat(prisonId).isEqualTo("WMI")
      Assertions.assertThat(prisonName).isEqualTo("Wymott (HMP & YOI)")
    }
  }

  @Test
  fun `should return prison names active only and ids by name order`() {
    // Given
    val endPoint = "/prisons/names?active=true"

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("ACI")
      Assertions.assertThat(prisonName).isEqualTo("Altcourse (HMP & YOI)")
    }
    with(prisonNames.last()) {
      Assertions.assertThat(prisonId).isEqualTo("WMI")
      Assertions.assertThat(prisonName).isEqualTo("Wymott (HMP & YOI)")
    }
  }

  @Test
  fun `should return prison non active names only and ids by name order`() {
    // Given
    val endPoint = "/prisons/names?active=false"

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("AKI")
      Assertions.assertThat(prisonName).isEqualTo("Acklington (HMP)")
    }
    with(prisonNames.last()) {
      Assertions.assertThat(prisonId).isEqualTo("WOI")
      Assertions.assertThat(prisonName).isEqualTo("Wolds (HMP)")
    }
  }

  @Test
  fun `should return prison active name id only`() {
    // Given
    val endPoint = "/prisons/names?name=WDI"

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
    with(prisonNames[0]) {
      Assertions.assertThat(prisonId).isEqualTo("WDI")
      Assertions.assertThat(prisonName).isEqualTo("Wakefield (HMP)")
    }
  }

  // Different combinations of 'active' and 'name por positive scenarios'
  @ParameterizedTest
  @CsvSource(
    "true, WDI",
    "null, WDI",
    "false, WOI",
    "null, WOI",
  )
  fun `should return prison names based on any kind of active value, name, positive scenarios`(active: String?, name: String?) {
    val queryParams = mutableListOf<String>()
    if (active != "null") {
      queryParams.add("active=$active")
    }

    if (name != "null") {
      queryParams.add("name=$name")
    }

    val endPoint = "/prisons/names?" + queryParams.joinToString("&")

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isNotEmpty
  }

  // Different combinations of 'active' and 'name por positive scenarios'
  @ParameterizedTest
  @CsvSource(
    "false, WDI",
    "true, WOI",
  )
  fun `should not return prison names based on any kind of active value, name, negative scenarios`(active: String?, name: String?) {
    val queryParams = mutableListOf<String>()
    if (active != "null") {
      queryParams.add("active=$active")
    }

    if (name != "null") {
      queryParams.add("name=$name")
    }

    val endPoint = "/prisons/names?" + queryParams.joinToString("&")

    // When
    val responseSpec = webTestClient.get().uri(endPoint)
      .exchange()

    // Then
    responseSpec.expectStatus().isOk

    val prisonNames = getPrisonNames(responseSpec.expectBody())
    Assertions.assertThat(prisonNames).isEmpty()
  }

  fun getPrisonNames(returnResult: WebTestClient.BodyContentSpec): Array<PrisonNameDto> {
    return objectMapper.readValue(returnResult.returnResult().responseBody, Array<PrisonNameDto>::class.java)
  }
}
