package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(NON_NULL)
data class AgencyAddressDto(
  @Schema(description = "Unique ID of the address", example = "10000") val id: Long,
  @Schema(description = "Address line 1", example = "Bawtry Road") val addressLine1: String?,
  @Schema(description = "Address line 2", example = "Hatfield Woodhouse") val addressLine2: String?,
  @Schema(description = "Village/Town/City", example = "Doncaster") val town: String?,
  @Schema(description = "County", example = "South Yorkshire") val county: String?,
  @Schema(description = "Postcode", example = "DN7 6BW") val postcode: String?,
  @Schema(description = "Country", example = "England") val country: String?,
)

@JsonInclude(NON_NULL)
data class AgencyPhoneDto(
  @Schema(description = "Unique ID of the phone number", example = "10000") val id: Long,
  @Schema(description = "Phone number", example = "0114 555 9898") val number: String?,
)

@JsonInclude(NON_NULL)
data class AgencyEmailDto(
  @Schema(description = "Unique ID of the email address", example = "10000") val id: Long,
  @Schema(description = "Email address", example = "example@example.com") val address: String?,
)

data class CodeDescription(val code: String, val description: String)

@Schema(description = "Phone Number Update Record")
@JsonInclude(NON_NULL)
data class CreateOrUpdatePhoneNumberDto(
  @Schema(description = "Phone number", example = "0114 555 9898", required = true)
  @field:NotBlank(message = "Phone number is required")
  @field:Size(max = 100, message = "Phone number must be no more than 100 characters")
  val number: String,
)

@Schema(description = "Email Address Update Record")
@JsonInclude(NON_NULL)
data class CreateOrUpdateEmailAddressDto(
  @Schema(description = "Email address", example = "example@example.com", required = true)
  @field:NotBlank(message = "Email address is required")
  @field:Size(max = 100, message = "Email address must be no more than 100 characters")
  @field:Email(message = "Email address is in an incorrect format")
  val address: String,
)
