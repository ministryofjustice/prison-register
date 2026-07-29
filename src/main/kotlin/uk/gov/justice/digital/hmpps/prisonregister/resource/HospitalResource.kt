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
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.HospitalService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/hospitals", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class HospitalResource(private val hospitalService: HospitalService) {
  @GetMapping("/id/{hospitalId}")
  @Operation(summary = "Get specified hospital", description = "Information on a specific hospital")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getHospitalFromId(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Hospital Id must be between 3 and 6 letters")
    hospitalId: String,
  ): HospitalDto = hospitalService.findById(hospitalId)
}

@Schema(description = "Hospital Information")
@JsonInclude(NON_NULL)
data class HospitalDto(
  @Schema(description = "Hospital ID", example = "NWCLYC") val hospitalId: String,
  @Schema(description = "Name", example = "N Staffs Youth Hospital - Newcastle") val hospitalName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Hospital - Newcastle under Lyme") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographic Region") val geographicalArea: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "Is high security restricted hospital") val highSecurity: Boolean,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)
