package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidPhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidWebAddress

@Schema(description = "Contact information for a prison department")
class UpdateContactDetailsDto(
  @Schema(description = "Department Type", example = "SOCIAL_VISIT", required = true)
  @field:NotNull
  val type: DepartmentType,
  @Schema(description = "email address, if null current value is removed", example = "aled.evans@moj.gov.uk", required = false)
  @field:Email
  val emailAddress: String? = null,
  @Schema(description = "Phone Number, if null current value is removed", example = "01348811539", required = false)
  @field:ValidPhoneNumber
  val phoneNumber: String? = null,
  @Schema(description = "Web address, if null current value is removed", example = "https://www.gov.uk/guidance/bristol-prison", required = false)
  @field:ValidWebAddress
  val webAddress: String? = null,
)
