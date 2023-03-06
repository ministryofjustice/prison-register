package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.Size

const val OMU = "offender-management-unit"
const val VCC = "videolink-conferencing-centre"
const val EMAIL_ADDRESS = "email-address"
const val PRISONS = "prisons"
const val PRISON_BY_ID = "$PRISONS/id/{prisonId}"
const val SECURE_PRISON_BY_ID = "secure/$PRISON_BY_ID"

@RestController
@Validated
class PrisonEmailResource(private val prisonService: PrisonService) {

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
    prisonService
      .getVccEmailAddress(prisonId)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()

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
    prisonService
      .getOmuEmailAddress(prisonId)
      ?.let { ResponseEntity.ok(it) }
      ?: ResponseEntity.notFound().build()

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
  ): ResponseEntity<Void> =
    when (prisonService.setVccEmailAddress(prisonId, emailAddress)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()

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
  ): ResponseEntity<Void> =
    when (prisonService.setOmuEmailAddress(prisonId, emailAddress)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()

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
    prisonService.deleteVccEmailAddress(prisonId)
    return ResponseEntity.noContent().build()
  }

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
    prisonService.deleteOmuEmailAddress(prisonId)
    return ResponseEntity.noContent().build()
  }
}
