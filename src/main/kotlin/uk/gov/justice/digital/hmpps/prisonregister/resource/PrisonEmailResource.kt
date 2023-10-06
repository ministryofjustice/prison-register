package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
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
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

private const val OMU = "offender-management-unit"
private const val VCC = "videolink-conferencing-centre"

private const val EMAIL_ADDRESS = "email-address"
private const val PRISONS = "prisons"
private const val PRISON_BY_ID = "$PRISONS/id/{prisonId}"
private const val SECURE_PRISON_BY_ID = "secure/$PRISON_BY_ID"

@RestController
@Validated
class PrisonEmailResource(private val prisonService: PrisonService) {

  @Suppress("KotlinDeprecation")
  @GetMapping(
    "/$SECURE_PRISON_BY_ID/$VCC/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Get a prison's Videolink Conferencing Centre email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the email address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a Videolink Conferencing Centre email address",
      ),
    ],
  )
  fun getEmailForVideoConferencingCentre(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<String> =
    prisonService.getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()

  @Suppress("KotlinDeprecation")
  @GetMapping(
    "/$SECURE_PRISON_BY_ID/$OMU/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Get a prison's Offender Management Unit email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the email address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a Offender Management Unit email address",
      ),
    ],
  )
  fun getEmailForOffenderManagementUnit(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<String> =
    prisonService.getEmailAddress(prisonId, OFFENDER_MANAGEMENT_UNIT)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()

  @GetMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Get a prison department's email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the email address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a email address for this department",
      ),
    ],
  )
  fun getEmailAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<String> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    val emailAddress = prisonService.getEmailAddress(prisonId, departmentType)
    return emailAddress?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity<String>(
        "Could not find email address for $prisonId and ${departmentType.pathVariable}.",
        HttpStatus.NOT_FOUND,
      )
  }

  @Suppress("KotlinDeprecation")
  @PutMapping(
    "/$SECURE_PRISON_BY_ID/$VCC/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Set or change a prison's Videolink Conferencing Centre email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The email address was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The email address was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, email address or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison found for the supplied prison id",
      ),
    ],
  )
  fun putEmailAddressForVideolinkConferencingCentre(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,

    @Schema(description = "Email address", example = "a@b.com", required = true)
    @RequestBody
    @Valid
    @Email
    emailAddress: String,
  ): ResponseEntity<Void> {
    return when (prisonService.setEmailAddress(prisonId, emailAddress, VIDEOLINK_CONFERENCING_CENTRE)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @Suppress("KotlinDeprecation")
  @PutMapping(
    "/$SECURE_PRISON_BY_ID/$OMU/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Set or change a prison's Offender Management Unit email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The email address was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The email address was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, email address, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison found for the supplied prison id",
      ),
    ],
  )
  fun putEmailAddressForOffenderManagementUnit(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,

    @Schema(description = "Email address", example = "a@b.com", required = true)
    @RequestBody
    @Valid
    @Email
    emailAddress: String,
  ): ResponseEntity<Void> {
    return when (prisonService.setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @PutMapping(
    "/$SECURE_PRISON_BY_ID/department/{departmentType}/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE],
  )
  @Operation(summary = "Set or change a prison department's email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The email address was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The email address was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, email address, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a email address for this department",
      ),
    ],
  )
  fun putEmailAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "Email address", example = "a@b.com", required = true)
    @RequestBody
    @Valid
    @Email
    emailAddress: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentTypeStr: String,
  ): ResponseEntity<Void> {
    val departmentType = DepartmentType.getFromPathVariable(departmentTypeStr)
    return when (prisonService.setEmailAddress(prisonId, emailAddress, departmentType)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @Suppress("KotlinDeprecation")
  @DeleteMapping("/$SECURE_PRISON_BY_ID/$VCC/$EMAIL_ADDRESS")
  @Operation(summary = "Remove a prison's Videolink Conferencing Centre email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The email address was removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
    ],
  )
  fun deleteEmailAddressForVideolinkConferencingCentre(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<Void> {
    prisonService.deleteEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
    return ResponseEntity.noContent().build()
  }

  @Suppress("KotlinDeprecation")
  @DeleteMapping("/$SECURE_PRISON_BY_ID/$OMU/$EMAIL_ADDRESS")
  @Operation(summary = "Remove a prison's Offender Management Unit email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The email address was removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
    ],
  )
  fun deleteEmailAddressForOffenderManagementUnit(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<Void> {
    prisonService.deleteEmailAddress(prisonId, OFFENDER_MANAGEMENT_UNIT)
    return ResponseEntity.noContent().build()
  }

  @DeleteMapping("/$SECURE_PRISON_BY_ID/department/{departmentType}/$EMAIL_ADDRESS")
  @Operation(summary = "Remove a prison department's email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The email address was removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a email address for this department",
      ),
    ],
  )
  fun deleteEmailAddress(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "DepartmentType", example = "social-visit", required = true)
    @PathVariable("departmentType")
    departmentType: String,
  ): ResponseEntity<Void> {
    prisonService.deleteEmailAddress(prisonId, DepartmentType.getFromPathVariable(departmentType), true)
    return ResponseEntity.noContent().build()
  }
}
