package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
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
class PrisonResource(private val prisonService: PrisonService) {
  @GetMapping("/$PRISON_BY_ID", produces = [MediaType.APPLICATION_JSON_VALUE])
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
  ): PrisonDto = prisonService.findById(prisonId)

  @GetMapping("/$PRISONS", produces = [MediaType.APPLICATION_JSON_VALUE])
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
  fun getPrisons(): List<PrisonDto> = prisonService.findAll()

  @GetMapping("/$PRISONS/search", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(summary = "Get prisons by search filter", description = "All prisons")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = arrayOf(
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonDto::class))
          )
        )
      )
    ]
  )
  fun getPrisonsBySearchFilter(
    @Parameter(description = "Active", example = "true", required = false) @RequestParam active: Boolean? = null,
    @Parameter(description = "Text search", example = "Sheffield", required = false) @RequestParam textSearch: String? = null,
    @Parameter(description = "Genders to filter by", example = "MALE, FEMALE", required = false) @RequestParam genders: List<Gender>? = listOf(),
  ): List<PrisonDto> = prisonService.findByPrisonFilter(active, textSearch, genders)

  @GetMapping(
    "/$SECURE_PRISON_BY_ID/$VCC/$EMAIL_ADDRESS",
    produces = [MediaType.TEXT_PLAIN_VALUE]
  )
  @Operation(summary = "Get a prison's Videolink Conferencing Centre email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the email address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar"
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a Videolink Conferencing Centre email address"
      ),
    ]
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
    produces = [MediaType.TEXT_PLAIN_VALUE]
  )
  @Operation(summary = "Get a prison's Offender Management Unit email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the email address",
        content = [Content(mediaType = MediaType.TEXT_PLAIN_VALUE)]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar"
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a Offender Management Unit email address"
      ),
    ]
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
    consumes = [MediaType.TEXT_PLAIN_VALUE]
  )
  @Operation(summary = "Set or change a prison's Videolink Conferencing Centre email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The email address was created"
      ),
      ApiResponse(
        responseCode = "204",
        description = "The email address was updated"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, email address or similar"
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison found for the supplied prison id"
      ),
    ]
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
    emailAddress: String
  ): ResponseEntity<Void> =
    when (prisonService.setVccEmailAddress(prisonId, emailAddress)) {
      SetOutcome.CREATED -> ResponseEntity.status(HttpStatus.CREATED)
      SetOutcome.UPDATED -> ResponseEntity.noContent()
    }.build()

  @PutMapping(
    "/$SECURE_PRISON_BY_ID/$OMU/$EMAIL_ADDRESS",
    consumes = [MediaType.TEXT_PLAIN_VALUE]
  )
  @Operation(summary = "Set or change a prison's Offender Management Unit email address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The email address was created"
      ),
      ApiResponse(
        responseCode = "204",
        description = "The email address was updated"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, email address, media type or similar"
      ),
      ApiResponse(
        responseCode = "404",
        description = "No prison found for the supplied prison id"
      ),
    ]
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
    emailAddress: String
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
        description = "The email address was removed"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar"
      ),
    ]
  )
  fun deleteEmailAddressForVideolinkConferencingCentre(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String
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
        description = "The email address was removed"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar"
      ),
    ]
  )
  fun deleteEmailAddressForOffenderManagementUnit(
    @Schema(description = "Prison ID", example = "MDI", required = true)
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
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean,
  @Schema(description = "Whether the prison has male prisoners") val male: Boolean? = null,
  @Schema(description = "Whether the prison has female prisoners") val female: Boolean? = null,
  @Schema(description = "List of address for this prison") val addresses: List<AddressDto> = listOf(),
) {
  constructor(prison: Prison) : this(
    prison.prisonId, prison.name, prison.active, prison.male, prison.female, prison.addresses.map { AddressDto(it) }
  )
}

data class AddressDto(
  @Schema(description = "Address line 1", example = "Bawtry Road", required = false) val addressLine1: String?,
  @Schema(description = "Address line 2", example = "Hatfield Woodhouse", required = false) val addressLine2: String?,
  @Schema(description = "Village/Town/City", example = "Doncaster", required = true) val town: String,
  @Schema(description = "County", example = "South Yorkshire", required = false) val county: String?,
  @Schema(description = "Country", example = "England", required = true) val country: String,
  @Schema(description = "Postcode", example = "DN7 6BW", required = true) val postcode: String,

) {
  constructor(address: Address) : this(
    address.addressLine1, address.addressLine2, address.town, address.county, address.country, address.postcode
  )
}
