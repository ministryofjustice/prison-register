package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.service.LegacySyncService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/legacy", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class LegacySyncResource(val telemetry: TelemetryClient, val legacySyncService: LegacySyncService) {

  @Operation(
    summary = "Creates or updates an agency of any type",
    description = "Used for synchronising data from NOMIS. This creates an agency, or updates it if it already exists. Role required is ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Bad Information request to create or update agency",
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
  @PostMapping("/sync/agency/id/{agencyId}")
  fun createOrUpdateAgency(
    @Schema(description = "NOMIS Agency Id", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 characters")
    agencyId: String,
    @RequestBody @Valid
    @Suppress("unused")
    agencyDto: LegacyAgencyDto,
  ): LegacyAgencyResponse = legacySyncService.createOrUpdateAgency(agencyId, agencyDto).also {
    // TODO raise appropriate domain events
    if (it.updated) {
      telemetry.trackEvent("legacy-sync-agency-updated", mapOf("agencyId" to agencyId), null)
    } else {
      telemetry.trackEvent("legacy-sync-agency-created", mapOf("agencyId" to agencyId), null)
    }
  }

  @Operation(
    summary = "Migrates an agency of any type",
    description = "Used for migrating data from NOMIS. This creates an agency, or updates it if it already exists. Role required is ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Bad Information request to create or update agency",
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
  @PostMapping("/migrate/agency/id/{agencyId}")
  fun migrateAgency(
    @Schema(description = "NOMIS Agency Id", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 characters")
    agencyId: String,
    @RequestBody @Valid
    @Suppress("unused")
    agencyDto: LegacyAgencyDto,
  ): LegacyAgencyResponse = legacySyncService.createOrUpdateAgency(agencyId, agencyDto).also {
    if (it.updated) {
      telemetry.trackEvent("legacy-migration-agency-updated", mapOf("agencyId" to agencyId), null)
    } else {
      telemetry.trackEvent("legacy-migration-agency-created", mapOf("agencyId" to agencyId), null)
    }
  }


  @Operation(
    summary = "Returns IDs of all non-prison agencies for reconciliation",
    description = "Returns the IDs of all courts, hospitals, probation offices, approved premises, police custody suites, and generic agencies. Role required is ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency IDs returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to retrieve agency IDs",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping("/reconciliation/ids/all")
  fun getAllAgencyIds(): AgencyIdsResponse = legacySyncService.getAllIds()

  @Operation(
    description = "Deletes all synchronized agency data except prisons. Role required is ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Agencies deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete agencies",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/admin/sync/agency/all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteAllAgencies() {
    legacySyncService.deleteAll()
  }
}

@Schema(description = "Agency Information")
@JsonInclude(NON_NULL)
data class LegacyAgencyDto(
  @Schema(description = "Agency Type", example = "COURT", enumAsRef = true) val agencyType: LegacyAgencyType,
  @Schema(description = "Name", example = "N Staffs Youth Court - Newcastle") val name: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Court - Newcastle under Lyme") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area Code", example = "NW") val areaCode: String?,
  @Schema(description = "Subarea Code", example = "SHEFF") val subareaCode: String? = null,
  @Schema(description = "Region Code", example = "YOHUM") val regionCode: String?,
  @Schema(description = "Geographic Region code", example = "WYORKS") val geographicalAreaCode: String?,
  @Schema(description = "Prisoner Payroll Region code", example = "HS") val payrollRegionCode: String?,
  @Schema(description = "Local Authority code", example = "00CG") val localAuthorityCode: String?,
  @Schema(description = "Court Type code", example = "CC") val courtTypeCode: String?,
  @Schema(description = "Accessible Access", example = "ACCESSIBLE") val accessibleAccess: LegacyAccessibleAccess?,
  @Schema(description = "Contact", example = "John Smith") val contact: String?,
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
  @Schema(description = "Phone number", example = "0114 555 9898") val number: String,
)

@JsonInclude(NON_NULL)
data class LegacyAgencyEmailDto(
  @Schema(description = "Email address", example = "example@example.com") val address: String,
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

@Suppress("unused")
enum class LegacyAccessibleAccess {
  NONE,
  ACCESSIBLE,
  BY_ARRANGEMENT_ONLY,
  WHEELCHAIR_ACCESS,
}

data class AgencyIdsResponse(val agencyIds: List<AgencyId>)
data class AgencyId(val agencyId: String)
