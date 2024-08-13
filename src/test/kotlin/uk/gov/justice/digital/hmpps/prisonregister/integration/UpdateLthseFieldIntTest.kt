package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService

class UpdateLthseFieldIntTest : IntegrationTest() {

  @SpyBean
  lateinit var prisonRepository: PrisonRepository

  @MockBean
  private lateinit var auditService: AuditService

  val prisonId = "HSE"

  @BeforeEach
  fun setupHighSecurityPrisons() {
    val lthsePrison = Prison(
      prisonId = prisonId,
      name = "LTHSE Prison",
      active = true,
      male = true,
      female = false,
      contracted = true,
      lthse = false,
    )
    prisonRepository.saveAndFlush(lthsePrison)
  }

  @Test
  fun `When we update a prisons lthse field, return that prison with updated lthse field`() {
    val endpoint = "/prison-maintenance/id/$prisonId"
    val headers = setAuthorisation(
      roles = listOf("ROLE_MAINTAIN_REF_DATA"),
      scopes = listOf("write"),
      user = "bobby.beans",
    )
    val dto = UpdatePrisonDto(
      prisonName = "LTHSE Prison",
      active = true,
      lthse = true,
    )
    val body = BodyInserters.fromValue(dto)

    val response = updateLthseField(endpoint, headers, body)
    assertThat(response.lthse).isTrue

    val dataFromDB = prisonRepository.findByPrisonId(prisonId)
    assertThat(dataFromDB!!.lthse).isTrue

    verify(auditService).sendAuditEvent(
      eq("PRISON_REGISTER_UPDATE"),
      eq(Pair(prisonId, dto)),
      any(),
    )
  }

  private fun updateLthseField(endpoint: String, headers: (HttpHeaders) -> Unit, body: BodyInserter<UpdatePrisonDto, ReactiveHttpOutputMessage>): PrisonDto {
    val responseSpec = webTestClient.put()
      .uri(endpoint)
      .headers(headers)
      .body(body)
      .exchange()

    responseSpec.expectStatus()
      .isOk

    return responseSpec.expectBody(PrisonDto::class.java)
      .returnResult().responseBody!!
  }
}
