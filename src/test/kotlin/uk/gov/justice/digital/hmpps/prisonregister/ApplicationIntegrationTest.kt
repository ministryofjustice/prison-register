package uk.gov.justice.digital.hmpps.prisonregister

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest

const val PRISON_ID = "LEI"
const val OMU_URI = "/secure/prisons/id/{prisonId}/offender-management-unit/email-address"
const val VCC_URI = "/secure/prisons/id/{prisonId}/videolink-conferencing-centre/email-address"
const val EMAIL_1 = "a@b.com"
const val EMAIL_2 = "d@e.org"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationIntegrationTest : IntegrationTest() {
  @Test
  fun `OMU email address life-cycle`() {
    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .exchange()
      .expectStatus().isUnauthorized

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isNotFound

    webTestClient
      .put()
      .uri(OMU_URI, PRISON_ID)
      .contentType(MediaType.TEXT_PLAIN)
      .headers { it.maintenanceToken() }
      .bodyValue(EMAIL_1)
      .exchange()
      .expectStatus().isCreated

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .exchange()
      .expectStatus().isUnauthorized

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java).isEqualTo<Nothing>(EMAIL_1)

    webTestClient
      .put()
      .uri(OMU_URI, PRISON_ID)
      .contentType(MediaType.TEXT_PLAIN)
      .headers { it.maintenanceToken() }
      .bodyValue(EMAIL_2)
      .exchange()
      .expectStatus().isNoContent

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java).isEqualTo<Nothing>(EMAIL_2)

    webTestClient
      .delete()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.maintenanceToken() }
      .exchange()
      .expectStatus().isNoContent

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `VCC email address life-cycle`() {
    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .exchange()
      .expectStatus().isUnauthorized

    webTestClient
      .get()
      .uri(OMU_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isNotFound

    webTestClient
      .put()
      .uri(VCC_URI, PRISON_ID)
      .contentType(MediaType.TEXT_PLAIN)
      .headers { it.maintenanceToken() }
      .bodyValue(EMAIL_1)
      .exchange()
      .expectStatus().isCreated

    webTestClient
      .get()
      .uri(VCC_URI, PRISON_ID)
      .exchange()
      .expectStatus().isUnauthorized

    webTestClient
      .get()
      .uri(VCC_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java).isEqualTo<Nothing>(EMAIL_1)

    webTestClient
      .put()
      .uri(VCC_URI, PRISON_ID)
      .contentType(MediaType.TEXT_PLAIN)
      .headers { it.maintenanceToken() }
      .bodyValue(EMAIL_2)
      .exchange()
      .expectStatus().isNoContent

    webTestClient
      .get()
      .uri(VCC_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java).isEqualTo<Nothing>(EMAIL_2)

    webTestClient
      .delete()
      .uri(VCC_URI, PRISON_ID)
      .headers { it.maintenanceToken() }
      .exchange()
      .expectStatus().isNoContent

    webTestClient
      .get()
      .uri(VCC_URI, PRISON_ID)
      .headers { it.readOnlyToken() }
      .exchange()
      .expectStatus().isNotFound
  }

  private fun HttpHeaders.readOnlyToken() {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "A_USER",
        roles = listOf(),
        clientId = "prison-register-client",
      ),
    )
  }

  private fun HttpHeaders.maintenanceToken() {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "A_USER",
        roles = listOf("ROLE_MAINTAIN_REF_DATA"),
        scope = listOf("write"),
        clientId = "prison-register-client",
      ),
    )
  }
}
