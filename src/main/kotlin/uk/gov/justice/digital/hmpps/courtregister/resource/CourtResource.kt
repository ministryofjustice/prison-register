package uk.gov.justice.digital.hmpps.courtregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtregister.jpa.Court
import uk.gov.justice.digital.hmpps.courtregister.service.CourtService
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/courts", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtResource(private val courtService: CourtService) {
  @GetMapping("/id/{courtId}")
  @Operation(summary = "Get specified court", description = "Information on a specific court")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = CourtDto::class))]
      )
    ]
  )
  fun getCourtFromId(
    @Schema(description = "Court ID", example = "ACCRYC", required = true)
    @PathVariable @Size(max = 12, min = 2) courtId: String
  ): CourtDto =
    courtService.findById(courtId)

  @GetMapping("")
  @Operation(summary = "Get all courts", description = "All courts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json")]
      )
    ]
  )
  fun getCourts(): List<CourtDto> =
    courtService.findAll()
}

@JsonInclude(NON_NULL)
@Schema(description = "Court Information", example = "Details about a court")
data class CourtDto(
  @Schema(description = "Court ID", example = "ACCRYC", required = true) val courtId: String,
  @Schema(description = "Name of the court", example = "Accrington Youth Court", required = true) val courtName: String,
  @Schema(description = "Description of the court", example = "Accrington Youth Court", required = false) val courtDescription: String?,
  @Schema(description = "Whether the court is still active", required = true) val active: Boolean
) {
  constructor(court: Court) : this(court.id, court.courtName, court.courtDescription, court.active)
}
