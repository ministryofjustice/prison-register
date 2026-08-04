package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/sync", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class LegacySyncResource {
  @Operation(
    summary = "Creates or updates a agency of any type",
    description = "Used for synchronising and migrating data from NOMIS this will create an agency or update if already exists. Role required is ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = LegacyAgencyDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency created or updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Information request to create or update update agency",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add or update agency",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/agency/id/{agencyId}")
  fun createOrUpdateAgency(
    @Schema(description = "NOMIS Agency Id", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Agency Id must be between 3 and 6 characters")
    agencyId: String,
    @RequestBody @Valid
    @Suppress("unused")
    agencyDto: LegacyAgencyDto,
  ): LegacyAgencyResponse = LegacyAgencyResponse(false)
}

@Schema(description = "Agency Information")
@JsonInclude(NON_NULL)
data class LegacyAgencyDto(
  @Schema(description = "Agency Type", example = "COURT", enumAsRef = true) val agencyType: LegacyAgencyType,
  @Schema(description = "Name", example = "N Staffs Youth Court - Newcastle") val courtName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Court - Newcastle under Lyme") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area Code", example = "NW") val areaCode: String?,
  @Schema(description = "Region Code", example = "YOHUM") val regionCode: String?,
  @Schema(description = "Geographic Region code", example = "WYORKS") val geographicalAreaCode: String?,
  @Schema(description = "Prisoner Payroll Region code", example = "HS") val payrollRegionCode: String?,
  @Schema(description = "Court Type code", example = "CC") val courtTypeCode: String?,
  @Schema(description = "addresses") val addresses: List<LegacyAgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<LegacyAgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<LegacyAgencyPhoneDto>,
)

@JsonInclude(NON_NULL)
data class LegacyAgencyAddressDto(
  @Schema(description = "Address line 1", example = "Bawtry Road") val addressLine1: String?,
  @Schema(description = "Address line 2", example = "Hatfield Woodhouse") val addressLine2: String?,
  @Schema(description = "Village/Town/City", example = "Doncaster") val town: String?,
  @Schema(description = "County", example = "South Yorkshire") val county: String?,
  @Schema(description = "Postcode", example = "DN7 6BW") val postcode: String?,
  @Schema(description = "Country", example = "England") val country: String?,
)

@JsonInclude(NON_NULL)
data class LegacyAgencyPhoneDto(
  @Schema(description = "Phone number", example = "0114 555 9898") val number: String?,
)

@JsonInclude(NON_NULL)
data class LegacyAgencyEmailDto(
  @Schema(description = "Email address", example = "example@example.com") val address: String?,
)

data class LegacyAgencyResponse(val updated: Boolean)

@Suppress("unused")
enum class LegacyAgencyType {
  PRISON,
  COURT,
  HOSPITAL,
  SECURE_HOSPITAL,
  PROBATION_OFFICE,
  POLICE_CUSTODY_SUITE,
  APPROVED_PREMISE,
  AIRPORT,
  PROBATION_CRC,
  FOREIGN_NATIONAL_PRISON,
  VOLUNTARY_HOSTEL,
  IMMIGRATION_DETENTION_CENTRE,
  OUTSIDE,
  PECS,
  PSYCHIATRIC_CARE,
  CHILDREN_SECURE_HOME,
  SECURE_TRAINING_CENTRE,
  YOT,
}
