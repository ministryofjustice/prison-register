package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidTelephoneAddress
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

private const val TELEPHONE_ADDRESS = "telephone-address"
private const val PRISONS = "prisons"
private const val PRISON_BY_ID = "$PRISONS/id/{prisonId}"
private const val SECURE_PRISON_BY_ID = "secure/$PRISON_BY_ID"

@RestController
@Validated
class PrisonTelephoneAddressResource(private val prisonService: PrisonService) {

  @GetMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$TELEPHONE_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Get a prison department's telephone address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the telephone address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a telephone address for this department",
      ),
    ],
  )
  fun getTelephoneAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<String> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    val telephoneAddress = prisonService.getTelephoneAddress(prisonId, departmentType)
    return telephoneAddress?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity<String>(
        "Could not find telephone address for $prisonId and ${departmentType.pathVariable}.",
        HttpStatus.NOT_FOUND,
      )
  }

  @PutMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$TELEPHONE_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Set or change a prison department's telephone address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The telephone address was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The telephone address was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, telephone address, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a telephone address for this department",
      ),
    ],
  )
  fun putTelephoneAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "A Valid telephone address", example = "01348811539", required = true)
    @RequestBody
    @Valid
    @ValidTelephoneAddress
    telephoneAddress: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<Void> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    return when (prisonService.setTelephoneAddress(prisonId, telephoneAddress, departmentType)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @DeleteMapping("/$SECURE_PRISON_BY_ID/department/{departmentType}/$TELEPHONE_ADDRESS")
  @Operation(summary = "Remove a prison department's telephone address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The telephone address was removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a telephone address for this department",
      ),
    ],
  )
  fun deleteTelephoneAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentType: String,
  ): ResponseEntity<Void> {
    prisonService.deleteTelephoneAddress(prisonId, DepartmentType.getFromPathVariable(departmentType), true)
    return ResponseEntity.noContent().build()
  }
}
