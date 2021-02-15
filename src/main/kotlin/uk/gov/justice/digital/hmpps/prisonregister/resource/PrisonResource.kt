package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.Size

const val OMU = "offender-management-unit"
const val VCC = "videolink-conferencing-centre"
const val EMAIL_ADDRESS = "email-address"
const val PRISON_BY_ID = "/id/{prisonId}"

@RestController
@Validated
@RequestMapping("/prisons", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonResource(private val prisonService: PrisonService) {
  @GetMapping("$PRISON_BY_ID")
  @Operation(summary = "Get specified prison", description = "Information on a specific prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonDto::class))]
      )
    ]
  )
  fun getPrisonFromId(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable @Size(max = 12, min = 2) prisonId: String
  ): PrisonDto =
    prisonService.findById(prisonId)

  @GetMapping("")
  @Operation(summary = "Get all prisons", description = "All prisons")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json")]
      )
    ]
  )
  fun getPrisons(): List<PrisonDto> =
    prisonService.findAll()

  @GetMapping(
    "$PRISON_BY_ID/$VCC/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE]
  )
  fun getEmailForVideoConferencingCentre(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<String> =
    prisonService
      .getVccEmailAddress(prisonId)
      .map { emailAddress -> ResponseEntity.ok(emailAddress) }
      .orElse(ResponseEntity.notFound().build())

  @GetMapping(
    "$PRISON_BY_ID/$OMU/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE]
  )
  fun getEmailForOffenderManagementUnit(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
  ): ResponseEntity<String> =
    prisonService
      .getOmuEmailAddress(prisonId)
      .map { emailAddress -> ResponseEntity.ok(emailAddress) }
      .orElse(ResponseEntity.notFound().build())

  @PutMapping(
    "$PRISON_BY_ID/$VCC/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE]
  )
  fun putEmailAddressForVideolinkConferencingCentre(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,

    @RequestBody
    @Valid
    @Email
    emailAddress: String
  ): ResponseEntity<Void> {
    val outcome = prisonService.setVccEmailAddress(prisonId, emailAddress)

    return when (outcome) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @PutMapping(
    "/$PRISON_BY_ID/$OMU/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE]
  )
  fun putEmailAddressForOffenderManagementUnit(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,

    @RequestBody
    @Valid
    @Email
    emailAddress: String
  ): ResponseEntity<Void> {
    val outcome = prisonService.setOmuEmailAddress(prisonId, emailAddress)

    return when (outcome) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()
  }

  @DeleteMapping("$PRISON_BY_ID/$VCC/$EMAIL_ADDRESS")
  fun deleteEmailAddressForVideolinkConferencingCentre(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String
  ): ResponseEntity<Void> {
    prisonService.deleteVccEmailAddress(prisonId)
    return ResponseEntity.noContent().build()
  }

  @DeleteMapping("$PRISON_BY_ID/$OMU/$EMAIL_ADDRESS")
  fun deleteEmailAddressForOffenderManagementUnit(
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String
  ): ResponseEntity<Void> {
    prisonService.deleteOmuEmailAddress(prisonId)
    return ResponseEntity.noContent().build()
  }
}

@JsonInclude(NON_NULL)
@Schema(description = "Prison Information", example = "Details about a prison")
data class PrisonDto(
  @Schema(description = "Prison ID", example = "MDI", required = true) val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true) val prisonName: String,
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean
) {
  constructor(prison: Prison) : this(prison.prisonId, prison.name, prison.active)
}
