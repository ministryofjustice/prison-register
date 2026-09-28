package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.PublicApiService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/api/agencies", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('HMPPS_REGISTERS_API__R')")
class PublicApiResource(
  private val publicApiService: PublicApiService,
) {
  @GetMapping
  @Operation(
    summary = "Get all agencies",
    description = "Summary information on all agencies, including prisons, probation offices, police custody suites, courts, hospitals and other agencies ordered by agency ID ascending",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getAgencies(): List<AgencySummaryDto> = publicApiService.getAll()

  @GetMapping("/{agencyId}")
  @Operation(
    summary = "Get details of an agency",
    description = "Details of a single agency of any type (court, hospital, police custody suite, probation office, approved premises, prison or other agency)",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAgency(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 characters")
    agencyId: String,
  ): AgencyDetailsDto = publicApiService.getAgency(agencyId)
}

@Schema(description = "Summary information about an agency")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencySummaryDto(
  @Schema(description = "Agency ID", example = "SHEFCC") val agencyId: String,
  @Schema(description = "Description", example = "Sheffield Central Court") val description: String?,
  @Schema(description = "Agency type", example = "COURT") val agencyType: LegacyAgencyType,
  @Schema(description = "Whether still active") val active: Boolean,
)

@Schema(description = "Details of an agency")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencyDetailsDto(
  @Schema(description = "Agency ID", example = "SHEFCC") val agencyId: String,
  @Schema(description = "Description", example = "Sheffield Crown Court") val description: String?,
  @Schema(description = "Long description", example = "Sheffield Central Court") val longDescription: String?,
  @Schema(description = "Agency type", example = "COURT") val agencyType: LegacyAgencyType,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Court Type code", example = "CC") val courtType: String? = null,
  @Schema(description = "Court Type description", example = "Crown Court") val courtTypeDescription: String? = null,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate? = null,
  @Schema(description = "Addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "Phone numbers") val phones: List<AgencyPhoneDto>,
  @Schema(description = "Email addresses") val emails: List<AgencyEmailDto>,
  @Schema(description = "Area") val area: CodeDescription? = null,
  @Schema(description = "Region") val region: CodeDescription? = null,
)
