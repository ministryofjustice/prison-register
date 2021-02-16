package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.nhaarman.mockito_kotlin.whenever
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import java.util.Optional
import javax.persistence.EntityNotFoundException

const val OMU_EMAIL_ADDRESS_PATH = "/prisons/id/{prisonId}/offender-management-unit/email-address"
const val VCC_EMAIL_ADDRESS_PATH = "/prisons/id/{prisonId}/videolink-conferencing-centre/email-address"

/**
 * Spring MVC tests. Requests and responses for parameter binding, validation and exception handling
 */
@WebMvcTest(PrisonResource::class)
@ActiveProfiles("test")
class PrisonResourceMVCTest(@Autowired val mvc: MockMvc) {
  @MockBean
  lateinit var prisonService: PrisonService

  @Test
  fun `get OMU email address`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn(Optional.of("a@b.com"))
    mvc.perform(
      get(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isOk)
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
      .andExpect(content().string("a@b.com"))
  }

  @Test
  fun `get OMU email address - Not found`() {
    whenever(prisonService.getOmuEmailAddress(anyString())).thenReturn(Optional.empty())
    mvc.perform(
      get(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT OMU email address - created`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isCreated)
  }

  @Test
  fun `PUT OMU email address - updated`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `PUT OMU email address - prison not found`() {
    whenever(prisonService.setOmuEmailAddress(anyString(), anyString())).thenThrow(EntityNotFoundException::class.java)

    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT OMU email address - invalid address`() {
    mvc.perform(
      put(OMU_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("xxxxxx")
    ).andExpect(status().is4xxClientError)
  }

  @Test
  fun `DELETE OMU email address`() {
    mvc.perform(
      delete(OMU_EMAIL_ADDRESS_PATH, "MDI")
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `get VCC email address`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn(Optional.of("a@b.com"))
    mvc.perform(
      get(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isOk)
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
      .andExpect(content().string("a@b.com"))
  }

  @Test
  fun `get VCC email address - Not found`() {
    whenever(prisonService.getVccEmailAddress(anyString())).thenReturn(Optional.empty())
    mvc.perform(
      get(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .accept(MediaType.TEXT_PLAIN)
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT VCC email address - created`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.CREATED)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isCreated)
  }

  @Test
  fun `PUT VCC email address - updated`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenReturn(SetOutcome.UPDATED)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNoContent)
  }

  @Test
  fun `PUT VCC email address - prison not found`() {
    whenever(prisonService.setVccEmailAddress(anyString(), anyString())).thenThrow(EntityNotFoundException::class.java)

    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("a@b.com")
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `PUT VCC email address - invalid address`() {
    mvc.perform(
      put(VCC_EMAIL_ADDRESS_PATH, "MDI")
        .contentType(MediaType.TEXT_PLAIN)
        .content("xxxxxx")
    ).andExpect(status().is4xxClientError)
  }

  @Test
  fun `DELETE VCC email address`() {
    mvc.perform(
      delete(VCC_EMAIL_ADDRESS_PATH, "MDI")
    ).andExpect(status().isNoContent)
  }
}
