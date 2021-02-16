package uk.gov.justice.digital.hmpps.prisonregister.model

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import java.util.Optional

const val ROLE_MAINTAIN_REF_DATA = "MAINTAIN_REF_DATA"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PrisonServiceSecurityTest(@Autowired val prisonService: PrisonService) {

  @MockBean
  lateinit var prisonRepository: PrisonRepository

  @MockBean
  lateinit var offenderManagementUnitRepository: OffenderManagementUnitRepository

  @MockBean
  lateinit var videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository

  @Test
  @WithMockUser(roles = [ROLE_MAINTAIN_REF_DATA])
  fun `Authorised user can update OMU email`() {
    whenever(offenderManagementUnitRepository.findById(ArgumentMatchers.anyString())).thenReturn(
      Optional.of(
        OffenderManagementUnit(Prison("MDI", "Moorland", true), "a@b.com")
      )
    )
    prisonService.setOmuEmailAddress("MDI", "a@b.com")
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not update OMU email`() {
    whenever(offenderManagementUnitRepository.findById(ArgumentMatchers.anyString())).thenReturn(
      Optional.of(
        OffenderManagementUnit(Prison("MDI", "Moorland", true), "a@b.com")
      )
    )
    assertThatThrownBy { prisonService.setOmuEmailAddress("MDI", "a@b.com") }
      .isInstanceOf(AccessDeniedException::class.java)
  }

  @Test
  @WithMockUser(roles = [ROLE_MAINTAIN_REF_DATA])
  fun `Authorised user can delete OMU email`() {
    prisonService.deleteOmuEmailAddress("MDI")
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not delete OMU email`() {
    assertThatThrownBy { prisonService.deleteOmuEmailAddress("MDI") }
      .isInstanceOf(AccessDeniedException::class.java)
  }

  @Test
  @WithMockUser(roles = [ROLE_MAINTAIN_REF_DATA])
  fun `Authorised user can update VCC email`() {
    whenever(videoLinkConferencingCentreRepository.findById(ArgumentMatchers.anyString())).thenReturn(
      Optional.of(
        VideolinkConferencingCentre(Prison("MDI", "Moorland", true), "a@b.com")
      )
    )
    prisonService.setVccEmailAddress("MDI", "a@b.com")
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not update VCC email`() {
    whenever(videoLinkConferencingCentreRepository.findById(ArgumentMatchers.anyString())).thenReturn(
      Optional.of(
        VideolinkConferencingCentre(Prison("MDI", "Moorland", true), "a@b.com")
      )
    )
    assertThatThrownBy { prisonService.setOmuEmailAddress("MDI", "a@b.com") }
      .isInstanceOf(AccessDeniedException::class.java)
  }

  @Test
  @WithMockUser(roles = [ROLE_MAINTAIN_REF_DATA])
  fun `Authorised user can delete VCC email`() {
    prisonService.deleteVccEmailAddress("MDI")
  }

  @Test
  @WithMockUser
  fun `An unauthorised user can not delete VCC email`() {
    assertThatThrownBy { prisonService.deleteVccEmailAddress("MDI") }.isInstanceOf(AccessDeniedException::class.java)
  }
}
