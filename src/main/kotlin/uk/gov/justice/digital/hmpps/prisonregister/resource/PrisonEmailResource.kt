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
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.OFFENDER_MANAGEMENT_UNIT
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType.VIDEOLINK_CONFERENCING_CENTRE
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import kotlin.DeprecationLevel.WARNING

private const val OMU = "offender-management-unit"
private const val VCC = "videolink-conferencing-centre"
private const val EMAIL_ADDRESS = "email-address"
private const val SECURE_PRISON_BY_ID = "secure/prisons/id/{prisonId}"

private const val CONTACT_DETAILS_END_POINT_VVC = "/$SECURE_PRISON_BY_ID/$VCC/$EMAIL_ADDRESS"
private const val CONTACT_DETAILS_END_POINT_OMU = "/$SECURE_PRISON_BY_ID/$OMU/$EMAIL_ADDRESS"
private const val CONTACT_DETAILS_END_POINT_DEPARTMENT = "/$SECURE_PRISON_BY_ID/department/{departmentType}/$EMAIL_ADDRESS"

@RestController
@Validated
class PrisonEmailResource(private val prisonService: PrisonService) {
  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @GetMapping(
    CONTACT_DETAILS_END_POINT_VVC,
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
  ): ResponseEntity<String> = prisonService.getEmailAddress(prisonId, VIDEOLINK_CONFERENCING_CENTRE)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()

  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @GetMapping(
    CONTACT_DETAILS_END_POINT_OMU,
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
  ): ResponseEntity<String> = prisonService.getEmailAddress(prisonId, OFFENDER_MANAGEMENT_UNIT)
    ?.let { ResponseEntity.ok(it) }
    ?: ResponseEntity.notFound().build()

  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @PutMapping(
    CONTACT_DETAILS_END_POINT_VVC,
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
    @Schema(description = "Email address", example = "aled.wynevans@digital.justice.gov.uk", required = true)
    @RequestBody
    @Valid
    @Email
    emailAddress: String,
  ): ResponseEntity<Void> = when (prisonService.setEmailAddress(prisonId, emailAddress, VIDEOLINK_CONFERENCING_CENTRE)) {
    SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
    SetOutcome.UPDATED -> ResponseEntity.noContent()
  }.build()

  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @PutMapping(
    CONTACT_DETAILS_END_POINT_OMU,
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
  ): ResponseEntity<Void> = when (prisonService.setEmailAddress(prisonId, emailAddress, OFFENDER_MANAGEMENT_UNIT)) {
    SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
    SetOutcome.UPDATED -> ResponseEntity.noContent()
  }.build()

  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @DeleteMapping(CONTACT_DETAILS_END_POINT_VVC)
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

  @Deprecated("This endpoint should be changed to corresponding $CONTACT_DETAILS_END_POINT_DEPARTMENT end point", ReplaceWith(CONTACT_DETAILS_END_POINT_DEPARTMENT), WARNING)
  @Suppress("KotlinDeprecation")
  @DeleteMapping(CONTACT_DETAILS_END_POINT_OMU)
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
}
