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
import uk.gov.justice.digital.hmpps.prisonregister.service.PoliceCustodySuiteService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/police-custody-suites", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class PoliceCustodySuiteResource(private val policeCustodySuiteService: PoliceCustodySuiteService) {
  @GetMapping("/id/{policeCustodySuiteId}")
  @Operation(summary = "Get specified police custody suite", description = "Information on a specific police custody suite")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getPoliceCustodySuiteFromId(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
  ): PoliceCustodySuiteDto = policeCustodySuiteService.findById(policeCustodySuiteId)
}

@Schema(description = "Police Custody Suite Information")
@JsonInclude(NON_NULL)
data class PoliceCustodySuiteDto(
  @Schema(description = "Police Custody Suite ID", example = "SHFPCS") val policeCustodySuiteId: String,
  @Schema(description = "Name", example = "Sheffield Police Custody Suite") val policeCustodySuiteName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Police Custody Suite") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographical Area") val geographicalArea: CodeDescription?,
  @Schema(description = "Local Authority") val localAuthority: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)
