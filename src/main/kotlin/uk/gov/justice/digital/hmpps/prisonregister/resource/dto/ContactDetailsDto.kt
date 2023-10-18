package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prisonregister.model.ContactDetails
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidPhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidWebAddress

@Schema(description = "Contact information for a prison department")
class ContactDetailsDto(
  @Schema(description = "Department Type", example = "SOCIAL_VISIT or PRISON", required = true)
  @field:NotNull
  val type: DepartmentType,
  @Schema(description = "email address", example = "example@example.com", required = false)
  @field:Email
  val emailAddress: String? = null,
  @Schema(description = "Phone Number", example = "01234567890", required = false)
  @field:ValidPhoneNumber
  val phoneNumber: String? = null,
  @Schema(description = "Web address", example = "https://www.example.co.uk", required = false)
  @field:ValidWebAddress
  val webAddress: String? = null,
) {

  constructor(contactDetails: ContactDetails) : this (
    type = contactDetails.type,
    contactDetails.emailAddress?.value,
    contactDetails.phoneNumber?.value,
    contactDetails.webAddress?.value,
  )
}
