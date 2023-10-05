package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactPurposeType.VIDEO_LINK_CONFERENCING
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.nio.charset.StandardCharsets

private const val OMU_URI = "/secure/prisons/id/{prisonId}/offender-management-unit/email-address"
private const val VCC_URI = "/secure/prisons/id/{prisonId}/videolink-conferencing-centre/email-address"
private const val PRISON_ID = "LEI"

class PutPrisonEmailResourceTest : IntegrationTest() {

  @SpyBean
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `When an email is updated, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"
    val oldEmailAddress = "old.aled@moj.com"
    val newEmailAddress = "new.aled@moj.com"
    createDBData(prisonId, contactPurposeType, oldEmailAddress)

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When an email is created, isCreated is return and persisted`() {
    // Given
    val newEmailAddress = "aled@moj.com"
    val prisonId = "BRI"
    val contactPurposeType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When an one email is added for more than one prison, only one email is persisted`() {
    // Given
    val newEmailAddress = "new.aled@moj.com"
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = "/secure/prisons/id/$prisonId1/type/${SOCIAL_VISIT.value}/email-address"
    val endPoint2 = "/secure/prisons/id/$prisonId2/type/${SOCIAL_VISIT.value}/email-address"
    val endPoint3 = "/secure/prisons/id/$prisonId2/type/${VIDEO_LINK_CONFERENCING.value}/email-address"

    // When
    val responseSpec1 = doStartAction(endPoint1, prisonId1, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)
    val responseSpec2 = doStartAction(endPoint2, prisonId2, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)
    val responseSpec3 = doStartAction(endPoint3, prisonId2, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec1.expectStatus().isCreated
    responseSpec2.expectStatus().isCreated
    responseSpec3.expectStatus().isCreated

    Assertions.assertThat(testEmailAddressRepository.getEmailCount(newEmailAddress)).isEqualTo(1)
  }

  @Test
  fun `When purpose type does not exist, then appropriate error is show`() {
    // Given
    val prisonId = "BRI"
    val endPoint = "/secure/prisons/id/$prisonId/type/i-do-not-exist/email-address"

    // When
    val responseSpec = doStartAction(endPoint, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    val bodyText = getResponseBodyText(responseSpec)
    org.junit.jupiter.api.Assertions.assertEquals(
      "Value for ContactPurposeType is not of a known type i-do-not-exist.",
      bodyText,
    )
  }

  @Test
  fun `When a new email request is sent with without a role, status Unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartActionNoRole(endPoint, prisonId)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  @Test
  fun `When a new email request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = SOCIAL_VISIT
    val endPoint = "/secure/prisons/id/$prisonId/type/${contactPurposeType.value}/email-address"

    // When
    val responseSpec = doStartAction(endPoint, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  @Test
  fun `When an email is updated for offender-management-unit, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = OFFENDER_MANAGEMENT_UNIT
    val oldEmailAddress = "aled@aled.com"
    val newEmailAddress = "aled@moj.com"
    createDBData(prisonId, contactPurposeType, oldEmailAddress)

    // When
    val responseSpec = doStartAction(OMU_URI, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When an email is created for offender-management-unit, isCreated is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = OFFENDER_MANAGEMENT_UNIT
    val newEmailAddress = "aled@moj.com"

    // When
    val responseSpec = doStartAction(OMU_URI, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated
    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When a new email request is sent for offender-management-unit without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = OFFENDER_MANAGEMENT_UNIT

    // When
    val responseSpec = doStartActionNoRole(OMU_URI, prisonId)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  @Test
  fun `When a new email request is sent for offender-management-unit with incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = OFFENDER_MANAGEMENT_UNIT

    // When
    val responseSpec = doStartAction(OMU_URI, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  @Test
  fun `When an email is updated for video-link-conferencing, isNoContent is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = VIDEO_LINK_CONFERENCING
    val oldEmailAddress = "aled@aled.com"
    val newEmailAddress = "aled@moj.com"
    createDBData(prisonId, contactPurposeType, oldEmailAddress)

    // When
    val responseSpec = doStartAction(VCC_URI, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isNoContent
    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When an email is created for video-link-conferencing, isCreated is return and the data is persisted`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = VIDEO_LINK_CONFERENCING
    val newEmailAddress = "aled@moj.com"

    // When
    val responseSpec = doStartAction(VCC_URI, prisonId, headers = createMaintainRoleWithWriteScope(), emailAddress = newEmailAddress)

    // Then
    responseSpec.expectStatus().isCreated

    assertDBValues(prisonId, newEmailAddress, contactPurposeType)
  }

  @Test
  fun `When a new email request is sent for video-link-conferencing without a role, status unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = VIDEO_LINK_CONFERENCING

    // When
    val responseSpec = doStartActionNoRole(VCC_URI, prisonId)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  @Test
  fun `When a new email request is sent for video-link-conferencing with incorrect role, status forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val contactPurposeType = VIDEO_LINK_CONFERENCING

    // When
    val responseSpec = doStartAction(VCC_URI, prisonId, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(emailAddressRepository)
    assertDBValuesAreNotPersisted(prisonId, contactPurposeType)
  }

  private fun assertDBValues(prisonId: String, newEmailAddress: String, type: ContactPurposeType) {
    Assertions.assertThat(emailAddressRepository.getEmailAddress(newEmailAddress)).isNotNull

    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    Assertions.assertThat(contactDetails).isNotNull
    contactDetails?.let {
      with(it) {
        Assertions.assertThat(prisonId).isEqualTo(prisonId)
        Assertions.assertThat(emailAddress.value).isEqualTo(newEmailAddress)
      }
    }
  }

  private fun assertDBValuesAreNotPersisted(prisonId: String, type: ContactPurposeType) {
    val contactDetails = contactDetailsRepository.getByPrisonIdAndType(prisonId, type)
    Assertions.assertThat(contactDetails).isNull()
  }
  private fun doStartActionNoRole(endPoint: String, uri: String): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, PRISON_ID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue("test@test.com")
      .exchange()
  }

  private fun doStartAction(endPoint: String, prisonID: String? = PRISON_ID, emailAddress: String ? = "a@a.com", headers: (HttpHeaders) -> Unit): ResponseSpec {
    return webTestClient
      .put()
      .uri(endPoint, prisonID)
      .contentType(MediaType.TEXT_PLAIN)
      .bodyValue(emailAddress)
      .headers(headers)
      .exchange()
  }

  private fun createAnyRole(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ANY_ROLE"), scopes = listOf("something"))

  private fun createMaintainRoleWithWriteScope(): (HttpHeaders) -> Unit = setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write"))

  private fun getResponseBodyText(responseSpec: ResponseSpec): String {
    return String(responseSpec.expectBody().returnResult().responseBody, StandardCharsets.UTF_8)
  }

  private fun createDBData(prisonId: String, contactPurposeType: ContactPurposeType, emailAddress: String = "aled@moj.gov.uk"): Prison {
    val prison = Prison(prisonId, "$prisonId Prison", active = true)
    prisonRepository.save(prison)

    val emailAddress = emailAddressRepository.save(EmailAddress(emailAddress))
    contactDetailsRepository.saveAndFlush(
      ContactDetails(
        prison.prisonId,
        prison,
        contactPurposeType,
        emailAddress,
      ),
    )

    return prison
  }
}
