package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.prisonregister.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetailsRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddressRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository

@WithMockUser(authorities = ["ROLE_MAINTAIN_REF_DATA", "SCOPE_write"])
annotation class WithMaintenanceMockUser

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PrisonServiceSecurityTest(@Autowired val prisonService: PrisonService) : IntegrationTest() {

  @MockBean
  lateinit var contactDetailsRepository: ContactDetailsRepository

  @MockBean
  lateinit var emailAddressRepository: EmailAddressRepository

  @MockBean
  lateinit var prisonRepository: PrisonRepository

  @MockBean
  lateinit var telemetryClient: TelemetryClient

  @Test
  @WithMaintenanceMockUser
  fun `Authorised user can update email`() {
    val contactDetails = ContactDetails(
      "MDI",
      Prison("MDI", "Moorland", active = true),
      OFFENDER_MANAGEMENT_UNIT,
      EmailAddress("a@b.com"),
    )

    whenever(contactDetailsRepository.getByPrisonIdAndType("MDI", contactDetails.type)).thenReturn(contactDetails)
    prisonService.setEmailAddress("MDI", "a@b.com", contactDetails.type)
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not update email`() {
    assertThatThrownBy { prisonService.setEmailAddress("MDI", "a@b.com", OFFENDER_MANAGEMENT_UNIT) }
      .isInstanceOf(AccessDeniedException::class.java)
  }

  @Test
  @WithMaintenanceMockUser
  fun `Authorised user can delete email`() {
    prisonService.deleteEmailAddress("MDI", OFFENDER_MANAGEMENT_UNIT)
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not delete email`() {
    assertThatThrownBy { prisonService.deleteEmailAddress("MDI", OFFENDER_MANAGEMENT_UNIT) }
      .isInstanceOf(AccessDeniedException::class.java)
  }
}
