package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonService
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/gp", produces = [MediaType.APPLICATION_JSON_VALUE])
class GpResource(private val prisonService: PrisonService) {
  @GetMapping("/prison/{prisonId}")
  @Operation(summary = "Get GP practise code about specified prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "400",
        description = "Bad request.  Wrong format for prison_id.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  fun getPrisonFromId(
    @Parameter(description = "Prison ID", example = "MDI")
    @PathVariable @Size(min = 2, max = 12) prisonId: String
  ): GpDto = GpDto(prisonService.findById(prisonId))

  @GetMapping("/practice/{gpPracticeCode}")
  @Operation(summary = "Get specified prison from GP practice code")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "400",
        description = "Bad request.  Wrong format for GP practice code.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison linked to the GP practice code.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  fun getPrisonFromGpPrescriber(
    @Parameter(description = "GP Practice Code", example = "Y05537")
    @PathVariable @Size(max = 6) gpPracticeCode: String
  ): GpDto = GpDto(prisonService.findByGpPractice(gpPracticeCode))
}

@Schema(description = "Prison and GP Practice Information")
@JsonInclude(NON_NULL)
data class GpDto(
  @Schema(description = "Prison ID", example = "MDI") val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland (HMP & YOI)") val prisonName: String,
  @Schema(description = "Whether the prison is still active") val active: Boolean,
  @Schema(description = "GP Practice Code", example = "Y05537") val gpPracticeCode: String?
) {
  constructor(prison: Prison) : this(prison.prisonId, prison.name, prison.active, prison.gpPractice?.gpPracticeCode)
}
