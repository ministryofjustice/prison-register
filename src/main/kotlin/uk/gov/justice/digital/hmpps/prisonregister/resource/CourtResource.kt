package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.CourtService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/courts", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class CourtResource(private val courtService: CourtService) {
  @GetMapping("/id/{courtId}")
  @Operation(summary = "Get specified court", description = "Information on a specific court")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getCourtFromId(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
  ): CourtDto = courtService.findById(courtId)
}

@Schema(description = "Court Information")
@JsonInclude(NON_NULL)
data class CourtDto(
  @Schema(description = "Court ID", example = "NWCLYC") val courtId: String,
  @Schema(description = "Name", example = "N Staffs Youth Court - Newcastle") val courtName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Court - Newcastle under Lyme") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "courtType") val courtType: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)
