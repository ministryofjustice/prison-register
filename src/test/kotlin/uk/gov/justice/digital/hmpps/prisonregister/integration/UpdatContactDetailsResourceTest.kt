package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto

class UpdatContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When contact details are updated, then http 200 is returned with persisted updated contact details`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.moj.gov.uk")
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811540", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, dto, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isOk
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())
    assertNotNull(contactDetailsDto)
    assertContactDetailsEquals(dto, contactDetailsDto)

    assertDbContactDetailsExist(prisonId, dto)
  }

  @Test
  fun `When more than one contact details are updated for more than one prison, only one of phone, email, web address is persisted`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getContactDetailsEndPoint(prisonId1)
    val endPoint2 = getContactDetailsEndPoint(prisonId2)

    val dto1 = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    val dto2 = ContactDetailsDto(OFFENDER_MANAGEMENT_UNIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811540", webAddress = "https://mojdigital.blog.gov.uk")

    createDBData(prisonId1, dto1)
    createDBData(prisonId2, dto2)

    // When
    val responseSpec1 = doUpdateContactDetailsAction(endPoint1, dto1, headers = createMaintainRoleWithWriteScope())
    val responseSpec2 = doUpdateContactDetailsAction(endPoint2, dto2, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec1.expectStatus().isOk
    responseSpec2.expectStatus().isOk

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(dto1.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(dto1.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(dto1.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When only phone number is updated and removeIfNull then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = true)
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, phoneNumber = "01348811539")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(createRequest, contactDetailsDto)

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
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(createRequest, contactDetailsDto)

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
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, webAddress = "www.aled.blog.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    assertContactDetailsEquals(createRequest, contactDetailsDto)

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
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, phoneNumber = "01348811539")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(persisted.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(persisted.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(createRequest.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When only email address is updated and removeIfNull false then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = false)
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "aled@moj.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(createRequest.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(persisted.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(persisted.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When only web address is updated and removeIfNull false then remove others but keep for others contact details`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "BRI"
    val endPoint1 = getContactDetailsEndPoint(prisonId1, removeIfNull = false)
    val createRequest = ContactDetailsDto(SOCIAL_VISIT, webAddress = "www.aled.blog.gov.uk")
    val persisted = createDBData(prisonId1, SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")
    createDBData(prisonId2, VIDEOLINK_CONFERENCING_CENTRE, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811539", webAddress = "www.mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint1, createRequest, headers = createMaintainRoleWithWriteScope())

    // Then
    val contactDetailsDto = getContactDetailsDtoResults(responseSpec.expectBody())

    Assertions.assertThat(persisted.type).isEqualTo(contactDetailsDto.type)
    Assertions.assertThat(persisted.emailAddress).isEqualTo(contactDetailsDto.emailAddress)
    Assertions.assertThat(createRequest.webAddress).isEqualTo(contactDetailsDto.webAddress)
    Assertions.assertThat(persisted.phoneNumber).isEqualTo(contactDetailsDto.phoneNumber)
  }

  @Test
  fun `When incorrect formats are used for contact details, then appropriate error messages are shown`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "I am not an email", phoneNumber = "I an bit a phone number", webAddress = "I am not a web address")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, dto, headers = createMaintainRoleWithWriteScope())

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
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811540", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, dto)

    // Then
    responseSpec.expectStatus().isUnauthorized
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = dto.type)
  }

  @Test
  fun `When a request is sent with an incorrect role, status Forbidden is returned`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811540", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, dto, headers = createAnyRole())

    // Then
    responseSpec.expectStatus().isForbidden
    verifyNoInteractions(contactDetailsRepository)
    verifyNoInteractions(phoneNumberRepository)
    assertContactDetailsHaveBeenDeleted(prisonId, department = dto.type)
  }

  @Test
  fun `When contact details are sent with a non existent prison, then bad request is returned`() {
    // Given
    val prisonId = "AWE"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01348811540", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doUpdateContactDetailsAction(endPoint, dto, headers = createMaintainRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound
    assertDeveloperMessage(responseSpec, "Unable to find uk.gov.justice.digital.hmpps.prisonregister.model.Prison with id AWE")
  }
}
