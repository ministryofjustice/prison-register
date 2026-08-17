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
import uk.gov.justice.digital.hmpps.prisonregister.service.AgencyService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/agencies", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class AgencyResource(private val agencyService: AgencyService) {
  @GetMapping("/id/{agencyId}")
  @Operation(summary = "Get specified agency", description = "Information on a specific agency")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getAgencyFromId(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
  ): AgencyDto = agencyService.findById(agencyId)
}

@Schema(description = "Agency Information")
@JsonInclude(NON_NULL)
data class AgencyDto(
  @Schema(description = "Agency ID", example = "SHEFCC") val agencyId: String,
  @Schema(description = "Name", example = "Sheffield Agency") val agencyName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Agency") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE") val accessibleAccess: String?,
  @Schema(description = "Agency type", example = "PROBATION_CRC") val agencyType: String,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographical Area") val geographicalArea: CodeDescription?,
  @Schema(description = "Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "Local Authority") val localAuthority: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)
