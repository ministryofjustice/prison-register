package uk.gov.justice.digital.hmpps.prisonregister.resource.model
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class PrisonRequest(
  @Schema(description = "List of prison ids", required = true)
  @field:Size(min = 3, max = 12, message = "Prison Id must be between 3 and 6 letters")
  val prisonIds: List<String>,
)
