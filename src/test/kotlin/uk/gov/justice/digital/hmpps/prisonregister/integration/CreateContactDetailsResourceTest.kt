package uk.gov.justice.digital.hmpps.prisonregister.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.SOCIAL_VISIT
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto

class CreateContactDetailsResourceTest : ContactDetailsBaseIntegrationTest() {

  @Test
  fun `When an contact details are created with maintain ref date role , isCreated is return and persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, dto)
  }

  @Test
  fun `When an contact details are created with maintain prison data role, isCreated is return and persisted`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createMaintainPrisonRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isCreated
    assertDbContactDetailsExist(prisonId, dto)
  }

  @Test
  fun `When contact details already exist, then bad request is returned`() {
    // Given
    val prisonId = "BRI"
    val departmentType = SOCIAL_VISIT
    val endPoint = getContactDetailsEndPoint(prisonId)
    createDBData(prisonId, departmentType, emailAddress = "aled@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.moj.gov.uk")
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isBadRequest
    assertDeveloperMessage(responseSpec, "Contact details already exist for BRI / social visit department.")
  }

  @Test
  fun `When more than one contactmdetails are created for more than one prison, only one of phone, email, web address is persisted`() {
    // Given
    val prisonId1 = "BRI"
    val prisonId2 = "CFI"

    val endPoint1 = getContactDetailsEndPoint(prisonId1)
    val endPoint2 = getContactDetailsEndPoint(prisonId2)

    val dto1 = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567890", webAddress = "www.mojdigital.blog.gov.uk")
    val dto2 = ContactDetailsDto(OFFENDER_MANAGEMENT_UNIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec1 = doCreateContactDetailsAction(endPoint1, prisonId1, dto1, headers = createMaintainRefRoleWithWriteScope())
    val responseSpec2 = doCreateContactDetailsAction(endPoint1, prisonId1, dto2, headers = createMaintainRefRoleWithWriteScope())
    val responseSpec3 = doCreateContactDetailsAction(endPoint2, prisonId2, dto1, headers = createMaintainRefRoleWithWriteScope())
    val responseSpec4 = doCreateContactDetailsAction(endPoint2, prisonId2, dto2, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec1.expectStatus().isCreated
    responseSpec2.expectStatus().isCreated
    responseSpec3.expectStatus().isCreated
    responseSpec4.expectStatus().isCreated

    Assertions.assertThat(testPhoneNumberRepository.getPhoneNumberCount(dto1.phoneNumber!!)).isEqualTo(1)
    Assertions.assertThat(testEmailAddressRepository.getEmailCount(dto1.emailAddress!!)).isEqualTo(1)
    Assertions.assertThat(testWebAddressRepository.getWebAddressCount(dto1.webAddress!!)).isEqualTo(1)
  }

  @Test
  fun `When incorrect formats are used for contact details, then appropriate error messages are shown`() {
    // Given
    val prisonId = "BRI"
    val endPoint = getContactDetailsEndPoint(prisonId)
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "I am not an email", phoneNumber = "I an bit a phone number", webAddress = "I am not a web address")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createMaintainRefRoleWithWriteScope())

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
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto)

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
    val dto = ContactDetailsDto(SOCIAL_VISIT, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createAnyRole())

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
    val dto = ContactDetailsDto(departmentType, emailAddress = "tom@moj.gov.uk", phoneNumber = "01234567880", webAddress = "https://mojdigital.blog.gov.uk")

    // When
    val responseSpec = doCreateContactDetailsAction(endPoint, prisonId, dto, headers = createMaintainRefRoleWithWriteScope())

    // Then
    responseSpec.expectStatus().isNotFound
    assertDeveloperMessage(responseSpec, "Unable to find uk.gov.justice.digital.hmpps.prisonregister.model.Prison with id AWE")
  }
}
