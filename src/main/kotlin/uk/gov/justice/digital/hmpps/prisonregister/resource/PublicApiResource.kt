package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.service.PublicApiService

@RestController
@Validated
@RequestMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
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
}

@Schema(description = "Summary information about an agency")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencySummaryDto(
  @Schema(description = "Agency ID", example = "SHEFCC") val agencyId: String,
  @Schema(description = "Description", example = "Sheffield Central Court") val description: String?,
  @Schema(description = "Agency type", example = "COURT") val agencyType: LegacyAgencyType,
  @Schema(description = "Whether still active") val active: Boolean,
)