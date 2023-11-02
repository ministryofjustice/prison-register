package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto

class UpdateContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When contact details are updated with maintain ref data role, then http 200 is returned with persisted updated contact details`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.moj.gov.uk")
    val updateRequest = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isOk
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertContactDetailsEquals(updateRequest, contactDetailsDto)

    assertDbContactDetailsExist(prisonId, updateRequest)
  }

  @Test
  fun `When contact details are updated with maintain prison data role, then http 200 is returned with persisted updated contact details`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.moj.gov.uk")
    val updateRequest = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainPrisonRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isOk
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertContactDetailsEquals(updateRequest, contactDetailsDto)

    assertDbContactDetailsExist(prisonId, updateRequest)
  }

  @Test
  fun `When more than one contact details are updated for more than one prison, only one of phone, email, web address is persisted`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getContactDetailsEndPoint(prisonId1)
    val endPoint2 = getContactDetailsEndPoint(prisonId2)

    val updateRequest1 = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    val updateRequest2 = ContactDetailsDto(OFFENDER_MANAGEMENT_UNIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    createDBData(prisonId1, updateRequest1)
    createDBData(prisonId2, updateRequest2)

    // When
    val responseSpec1 = doUpdateContactDetailsAction(endPoint1, updateRequest1, headers = createMaintainRefRoleWithWriteScope())
    val responseSpec2 = doUpdateContactDetailsAction(endPoint2, updateRequest2, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec1.expectStatus().isOk
    responseSpec2.expectStatus().isOk

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(updateRequest1.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(updateRequest1.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(updateRequest1.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When only phone number is updated and removeIfNull then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = true)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, phoneNumber = "01234567890")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(updateRequest, contactDetailsDto)

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(persisted.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(persisted.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(persisted.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When only email address is updated and removeIfNull then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = true)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(updateRequest, contactDetailsDto)

    Assertions.assertThat(testEmailAddressRepository.getEmailCount(persisted.emailAddress!!)).isEqualTo(1)

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(persisted.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(persisted.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(persisted.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When only web address is updated and removeIfNull then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = true)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, webAddress = "www.aled.blog.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(updateRequest, contactDetailsDto)

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(persisted.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(persisted.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(persisted.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When only phone number is updated and removeIfNull false then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = false)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, phoneNumber = "01234567890")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(persisted.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(persisted.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(updateRequest.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When only email address is updated and removeIfNull false then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = false)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(updateRequest.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(persisted.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(persisted.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When only contact details are orphaned and false, orphaned details are removed`() {
    // Given
    val endPoint = getContactDetailsEndPoint(prisonId, removeIfNull = false)
    val originalData = createDBData(prisonId, SOCIAL_VISIT, emailAddress = "delete-me@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.deleteMe.gov.uk")
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk", phoneNumber = "01234567880", webAddress = "www.aled.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isOk

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(originalData.phoneNumber!!)).isEqualTo(0)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(originalData.emailAddress!!)).isEqualTo(0)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(originalData.webAddress!!)).isEqualTo(0)
  }

  @Test
  fun `When only contact details are orphaned and true, orphaned details are removed`() {
    // Given
    val endPoint = getContactDetailsEndPoint(prisonId, removeIfNull = true)
    val originalData = createDBData(prisonId, SOCIAL_VISIT, emailAddress = "delete-me@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.deleteMe.gov.uk")
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk", phoneNumber = "01234567880", webAddress = "www.aled.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isOk

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(originalData.phoneNumber!!)).isEqualTo(0)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(originalData.emailAddress!!)).isEqualTo(0)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(originalData.webAddress!!)).isEqualTo(0)
  }

  @Test
  fun `When only web address is updated and removeIfNull false then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = false)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, webAddress = "www.aled.blog.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(persisted.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(updateRequest.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(persisted.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When incorrect formats are used for contact details, then appropriate error messages are shown`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "I am not an email", phoneNumber = "I an bit a phone number", webAddress = "I am not a web address")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus()
      .isBadRequest

    responseSpec.expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Must be a well-formed email address, Phone number is in an incorrect format, Web address is in an incorrect format")
  }

  @Test
  fun `When a request is sent with without a role, status Unauthorized is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = updateRequest.type)
  }

  @Test
  fun `When a request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val updateRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = updateRequest.type)
  }

  @Test
  fun `When contact details are sent with a non existent prison, then bad request is returned`() {
    // Given
    val prisonId = "AWE"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    val updateRequest = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, updateRequest, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound
    assertDeveloperMessage(responseSpec, "Unable to find uk.gov.justice.digital.hmpps.prisonregister.model.Prison with id AWE")
  }
}
