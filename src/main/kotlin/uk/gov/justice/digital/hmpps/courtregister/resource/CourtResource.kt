package uk.gov.justice.digital.hmpps.courtregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.courtregister.ErrorResponse
import uk.gov.justice.digital.hmpps.courtregister.jpa.Court
import uk.gov.justice.digital.hmpps.courtregister.service.CourtService
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/courts", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtResource(private val courtService: CourtService) {
  @GetMapping("/id/{courtId}")
  @ApiOperation("Get specified court")
  @ApiResponses(value = [
    ApiResponse(code = 400, message = "Bad request.  Wrong format for court ID.", response = ErrorResponse::class),
    ApiResponse(code = 404, message = "Court not found.", response = ErrorResponse::class)
  ])
  fun getCourtFromId(@ApiParam("Court ID", example = "ACTNYC") @PathVariable @Size(max = 12, min = 2) courtId: String): CourtDto =
      courtService.findById(courtId)

  @GetMapping("")
  @ApiOperation("Get all courts")
  fun getCourts(): List<CourtDto> =
    courtService.findAll()
}

@ApiModel("Court Information")
@JsonInclude(NON_NULL)
data class CourtDto(
    @ApiModelProperty("Court ID", example = "ACCRYC", position = 1, required = true) val courtId: String,
    @ApiModelProperty("Name of the court", example = "Accrington Youth Court", position = 2, required = true) val courtName: String,
    @ApiModelProperty("Description of the court", example = "Accrington Youth Court", position = 3, required = false) val courtDescription: String?,
    @ApiModelProperty("Whether the court is still active", position = 4, required = true) val active: Boolean
) {
  constructor(court: Court) : this(court.id, court.courtName, court.courtDescription, court.active)
}
