package uk.gov.justice.digital.hmpps.prisonregister.resource.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType

@Schema(description = "Request to for contact information for a prison department")
class ContactDetailsRequestDto(
  @Schema(description = "Department Type to delete", example = "SOCIAL_VISIT", required = true)
  @field:NotNull
  val type: DepartmentType,
)
