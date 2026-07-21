package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema

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
