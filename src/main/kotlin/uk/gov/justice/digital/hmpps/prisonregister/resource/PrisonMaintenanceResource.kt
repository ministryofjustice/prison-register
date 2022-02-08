package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.model.UpdatePrisonDto
import javax.validation.Valid
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping(name = "Prison Maintenance", path = ["/prison-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonMaintenanceResource(
  private val prisonService: PrisonService
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Update specified prison details",
    description = "Updates prison information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdatePrisonDto::class)
        )
      ]
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prison Information Updated",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonDto::class))]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to update prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make prison update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @PutMapping("/id/{prisonId}")
  fun updatePrison(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable @Size(max = 6, min = 3, message = "Prison Id must be between 3 and 6") prisonId: String,
    @RequestBody @Valid prisonUpdateRecord: UpdatePrisonDto
  ): PrisonDto {
    return prisonService.updatePrison(prisonId, prisonUpdateRecord)
  }
}
