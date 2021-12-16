package uk.gov.justice.digital.hmpps.prisonregister.resource

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.utilities.JwtAuthHelper
import javax.persistence.EntityNotFoundException

const val OMU_EMAIL_ADDRESS_PATH = "/secure/prisons/id/{prisonId}/offender-management-unit/email-address"
const val VCC_EMAIL_ADDRESS_PATH = "/secure/prisons/id/{prisonId}/videolink-conferencing-centre/email-address"

/**
 * Spring MVC tests. Requests and responses for parameter binding, validation and exception handling
 */
@WebMvcTest(PrisonResource::class)
@Import(JwtAuthHelper::class)
@ActiveProfiles("test")
class PrisonResourceMvcTest(@Autowired val mvc: MockMvc, @Autowired val jwtAuthHelper: JwtAuthHelper) {

  @MockBean
  lateinit var prisonService: PrisonService

  @Test
  fun `get OMU email address`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn("a@b.com")
    mvc.perform(
      get(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isOk)
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
      .andExpect(content().string("a@b.com"))
  }

  @Test
  fun `get OMU email address - Not found`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn(null)
    mvc.perform(
      get(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `get OMU email address - unauthorised`() {
    mvc.perform(
      get(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `PUT OMU email address - created`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isCreated)
  }

  @Test
  fun `PUT OMU email address - updated`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `PUT OMU email address - prison not found`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenThrow(EntityNotFoundException::class.java)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT OMU email address - unauthorised`() {
    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `PUT OMU email address - invalid address`() {
    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("xxxxxx")
    ).andExpect(status().is4xxClientError)
  }

  @Test
  fun `DELETE OMU email address`() {
    mvc.perform(
      delete(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `DELETE OMU email address - unauthorised`() {
    mvc.perform(
      delete(OMU_EMAIL_ADDRESS_PATH, "MDI")
    ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `get VCC email address`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn("a@b.com")
    mvc.perform(
      get(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isOk)
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
      .andExpect(content().string("a@b.com"))
  }

  @Test
  fun `get VCC email address - Not found`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn(null)
    mvc.perform(
      get(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `get VCC email address - Unauthorised`() {
    mvc.perform(
      get(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `PUT VCC email address - created`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isCreated)
  }

  @Test
  fun `PUT VCC email address - updated`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `PUT VCC email address - prison not found`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenThrow(EntityNotFoundException::class.java)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT VCC email address - unauthorised`() {
    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isUnauthorized)
  }

  @Test
  fun `PUT VCC email address - invalid address`() {
    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
        .contentType(MediaType.TEXT_PLAIN)
        .content("xxxxxx")
    ).andExpect(status().is4xxClientError)
  }

  @Test
  fun `DELETE VCC email address`() {
    mvc.perform(
      delete(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .authorise()
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `DELETE VCC email address - unauthorised`() {
    mvc.perform(
      delete(VCC_EMAIL_ADDRESS_PATH, "MDI")
    ).andExpect(status().isUnauthorized)
  }

  private fun MockHttpServletRequestBuilder.authorise(): MockHttpServletRequestBuilder {
    val token = jwtAuthHelper.createJwt(
      subject = "A_USER",
      roles = listOf(),
      clientId = "prison-register"
    )

    this.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    return this
  }
}
