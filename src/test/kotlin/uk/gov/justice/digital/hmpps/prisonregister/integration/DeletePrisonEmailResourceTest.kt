package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.nio.charset.StandardCharsets

private const val OMU_URI = "/secure/prisons/id/{prisonId}/offender-management-unit/email-address"
private const val VCC_URI = "/secure/prisons/id/{prisonId}/videolink-conferencing-centre/email-address"
private const val PRISON_ID = "LEI"
class DeletePrisonEmailResourceTest : IntegrationTest() {

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When an email is deleted for offender-management-unit, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = OFFENDER_MANAGEMENT_UNIT
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress)

    // When
    val responseSpec = doStartAction(OMU_URI, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValuesHaveBeenDeleted(prisonId, emailAddress, departmentType)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit without a role, status unauthorized is returned`() {
    // Given

    // When
    val responseSpec = doStartActionNoRole(OMU_URI)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for offender-management-unit with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"

    // When
    val responseSpec = doStartAction(OMU_URI, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When email address cannot be found for prison, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${departmentType.pathVariable}/email-address"

    // When
    val responseSpec = doStartAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals("Contact not found for prison ID BRI type social-visit.", bodyText)
  }

  @Test
  fun `When an email is deleted for video-link-conferencing, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = VIDEOLINK_CONFERENCING_CENTRE
    val emailAddress = "aled@aled.com"
    createDBData(prisonId, departmentType, emailAddress)

    // When
    val responseSpec = doStartAction(VCC_URI, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValuesHaveBeenDeleted(prisonId, emailAddress, departmentType)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given

    // When
    val responseSpec = doStartActionNoRole(VCC_URI)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested for video-link-conferencing with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"

    // When
    val responseSpec = doStartAction(VCC_URI, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email is deleted, isNoContent is returned and data is deleted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val emailAddress = "aled@aled.com"
    val endPoint = "/secure/prisons/id/$prisonId/type/${departmentType.pathVariable}/email-address"

    createDBData(prisonId, departmentType, emailAddress)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValuesHaveBeenDeleted(prisonId, emailAddress, departmentType)
  }

  @Test
  fun `When an email deletion has been requested without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${departmentType.pathVariable}/email-address"
    // When
    val responseSpec = doStartActionNoRole(endPoint)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When an email deletion has been requested with an incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${departmentType.pathVariable}/email-address"
    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
  }

  @Test
  fun `When department type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/type/i-do-not-exist/email-address"

    // When
    val responseSpec = doStartAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    assertEquals(
      "Value for DepartmentType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }

  private fun assertDBValuesHaveBeenDeleted(prisonId: String, newEmailAddress: String, type: DepartmentType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    Assertions.assertThat(contactDetails).isNull()
    val emailAddress = emailAddressRepository.getEmailAddress(newEmailAddress)
    Assertions.assertThat(emailAddress).isNull()
  }

  private fun doStartActionNoRole(endPoint: String): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, PRISON_ID)
      .exchange()
  }

  private fun doStartAction(endPoint: String, prisonID: String? = PRISON_ID, headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .delete()
      .uri(endPoint, prisonID)
      .headers(headers)
      .exchange()
  }

  private fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  private fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  private fun getResponseBodyText(responseSpec: ResponseSpec): String {
    val responseBody = responseSpec.expectBody().returnResult().responseBody
    Assertions.assertThat(responseBody).isNotNull
    return String(responseBody, StandardCharsets.UTF_8)
  }

  private fun createDBData(prisonId: String, departmentType: DepartmentType, emailAddress: String = "aled@moj.gov.uk"): Prison {
    val prison = Prison(prisonId, "$prisonId Prison", active = true)
    prisonRepository.save(prison)

    val persistedEmailAddress = emailAddressRepository.save(EmailAddress(emailAddress))
    contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        departmentType,
        persistedEmailAddress,
      ),
    )

    return prison
  }
}
