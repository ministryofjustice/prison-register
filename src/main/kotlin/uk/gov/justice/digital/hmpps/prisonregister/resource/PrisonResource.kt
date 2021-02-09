package uk.gov.justice.digital.hmpps.prisonregister.resource

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
import uk.gov.justice.digital.hmpps.prisonregister.jpa.Prison
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/prisons", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonResource(private val prisonService: PrisonService) {
  @GetMapping("/id/{prisonId}")
  @Operation(summary = "Get specified prison", description = "Information on a specific prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonDto::class))]
      )
    ]
  )
  fun getPrisonFromId(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable @Size(max = 12, min = 2) prisonId: String
  ): PrisonDto =
    prisonService.findById(prisonId)

  @GetMapping("")
  @Operation(summary = "Get all prisons", description = "All prisons")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json")]
      )
    ]
  )
  fun getPrisons(): List<PrisonDto> =
    prisonService.findAll()
}

@JsonInclude(NON_NULL)
@Schema(description = "Prison Information", example = "Details about a prison")
data class PrisonDto(
  @Schema(description = "Prison ID", example = "MDI", required = true) val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true) val prisonName: String,
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean
) {
  constructor(prison: Prison) : this(prison.prisonId, prison.name, prison.active)
}
