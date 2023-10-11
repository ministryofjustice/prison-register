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
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidPhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

private const val PHONE_NUMBER = "phone-number"
private const val PRISONS = "prisons"
private const val PRISON_BY_ID = "$PRISONS/id/{prisonId}"
private const val SECURE_PRISON_BY_ID = "secure/$PRISON_BY_ID"

@RestController
@Validated
class PrisonPhoneNumberResource(private val prisonService: PrisonService) {

  @GetMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$PHONE_NUMBER",
    produces = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Get a prison department's phone number")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the phone number",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun getPhoneNumber(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<String> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    val phoneNumber = prisonService.getPhoneNumber(prisonId, departmentType)
    return phoneNumber?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity<String>(
        "Could not find phone number for $prisonId and ${departmentType.pathVariable}.",
        HttpStatus.NOT_FOUND,
      )
  }

  @PutMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$PHONE_NUMBER",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Set or change a prison department's phone number")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The phone number was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The phone number was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, phone number, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun putPhoneNumber(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "A Valid phone number", example = "01348811539", required = true)
    @RequestBody
    @Valid
    @ValidPhoneNumber
    phoneNumber: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<Void> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    return when (prisonService.setPhoneNumber(prisonId, phoneNumber, departmentType)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @DeleteMapping("/$SECURE_PRISON_BY_ID/department/{departmentType}/$PHONE_NUMBER")
  @Operation(summary = "Remove a prison department's phone number")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The phone number was removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun deletePhoneNumber(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentType: String,
  ): ResponseEntity<Void> {
    prisonService.deletePhoneNumber(prisonId, DepartmentType.getFromPathVariable(departmentType), true)
    return ResponseEntity.noContent().build()
  }
}
