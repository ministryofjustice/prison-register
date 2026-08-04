package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTestBase

class LegacySyncResourceIntTest : IntegrationTestBase() {
  companion object {
    fun legacyAgencyDto() = LegacyAgencyDto(
      agencyType = LegacyAgencyType.COURT,
      courtName = "N Staffs Youth Court - Newcastle",
      description = "North Staffordshire Youth Court - Newcastle under Lyme",
      active = true,
      inactiveDate = null,
      cjitCode = "123456789",
      areaCode = "NW",
      regionCode = "YOHUM",
      geographicalAreaCode = "WYORKS",
      payrollRegionCode = "HS",
      courtTypeCode = "CC",
      addresses = listOf(),
      emailAddresses = listOf(),
      phoneNumbers = listOf(),
    )
  }

  @Nested
  inner class Security {
    @Test
    fun `requires a valid authentication token`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFCC")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `requires correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFCC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("BANANAS")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `allowed with correct role`() {
      webTestClient.post()
        .uri("/sync/agency/id/{agencyId}", "SHEFCC")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("HMPPS_REGISTERS_API__SYNCHRONISATION__RW")))
        .bodyValue(legacyAgencyDto())
        .exchange()
        .expectStatus().isOk
    }
  }
}
