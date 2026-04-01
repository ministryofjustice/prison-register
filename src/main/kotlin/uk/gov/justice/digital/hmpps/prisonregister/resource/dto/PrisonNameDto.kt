package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Full name of prison with id")
@JsonInclude(NON_NULL)
class PrisonNameDto(
  @Schema(description = "Prison ID", example = "MDI", required = true)
  val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true)
  val prisonName: String,
  @Schema(description = "Name of the prison in Welsh", example = "Carchar Brynbuga", required = false)
  val prisonNameInWelsh: String? = null,
)
