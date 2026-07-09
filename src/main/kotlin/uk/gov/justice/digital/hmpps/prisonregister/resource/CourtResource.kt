package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
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
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonDto::class))],
      ),
    ],
  )
  fun getCourtFromId(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Court Id must be between 3 and 6 letters")
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
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "courtType") val courtType: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)

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
