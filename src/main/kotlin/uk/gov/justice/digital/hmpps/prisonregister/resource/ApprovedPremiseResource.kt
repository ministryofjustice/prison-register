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
import uk.gov.justice.digital.hmpps.prisonregister.service.ApprovedPremiseService
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/approved-premises", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class ApprovedPremiseResource(private val approvedPremiseService: ApprovedPremiseService) {
  @GetMapping("/id/{approvedPremiseId}")
  @Operation(summary = "Get specified approved premise", description = "Information on a specific approved premise")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getApprovedPremiseFromId(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Approved Premise Id must be between 3 and 6 letters")
    approvedPremiseId: String,
  ): ApprovedPremiseDto = approvedPremiseService.findById(approvedPremiseId)
}

@Schema(description = "Approved Premise Information")
@JsonInclude(NON_NULL)
data class ApprovedPremiseDto(
  @Schema(description = "Approved Premise ID", example = "SHEFAP") val approvedPremiseId: String,
  @Schema(description = "Name", example = "Sheffield Approved Premise") val approvedPremiseName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premise") val description: String?,
  @Schema(description = "Contact", example = "John Smith") val contact: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE") val accessibleAccess: String?,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographical Area") val geographicalArea: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)
