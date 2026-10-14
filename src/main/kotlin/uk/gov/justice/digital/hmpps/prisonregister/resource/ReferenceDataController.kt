package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.ReferenceDataService

@RestController
@RequestMapping("/reference-data", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('HMPPS_REGISTERS_API__MAINTAIN__RW')")
@ApiResponses(
  value = [
    ApiResponse(responseCode = "200", description = "Successful Operation"),
    ApiResponse(
      responseCode = "401",
      description = "Unauthorized to access this endpoint",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Missing required role. Requires the HMPPS_REGISTERS_API__MAINTAIN__RW role",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  ],
)
class ReferenceDataController(
  private val referenceDataService: ReferenceDataService,
) {
  @GetMapping("/areas")
  @Operation(summary = "Get areas", description = "Returns areas as a flat list of code and description, optionally filtered by parent region code")
  fun getAreas(@RequestParam(required = false) region: String?): List<CodeDescription> = if (region == null) {
    referenceDataService.getAreas()
  } else {
    referenceDataService.getAreasByRegion(region)
  }

  @GetMapping("/regions")
  @Operation(summary = "Get all regions", description = "Returns all regions as a flat list of code and description")
  fun getRegions(): List<CodeDescription> = referenceDataService.getRegions()

  @GetMapping("/payroll-regions")
  @Operation(summary = "Get all payroll regions", description = "Returns all payroll regions as a flat list of code and description")
  fun getPayrollRegions(): List<CodeDescription> = referenceDataService.getPayrollRegions()

  @GetMapping("/local-authorities")
  @Operation(summary = "Get all local authorities", description = "Returns all local authorities as a flat list of code and description")
  fun getLocalAuthorities(): List<CodeDescription> = referenceDataService.getLocalAuthorities()

  @GetMapping("/subareas")
  @Operation(summary = "Get subareas by area", description = "Returns subareas with the given parent area code as a flat list of code and description")
  fun getSubareasByArea(@RequestParam area: String): List<CodeDescription> = referenceDataService.getSubareasByArea(area)

  @GetMapping("/suggestions/counties")
  @Operation(summary = "Get all county suggestions", description = "Returns all county suggestions as a flat list of code and description")
  fun getCountySuggestions(): List<CodeDescription> = referenceDataService.getCountySuggestions()

  @GetMapping("/suggestions/towns")
  @Operation(summary = "Get all town suggestions", description = "Returns all town suggestions as a flat list of code and description")
  fun getTownSuggestions(): List<CodeDescription> = referenceDataService.getTownSuggestions()
}
