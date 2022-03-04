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
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import javax.validation.constraints.Size

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
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonDto::class))
          )
        ]
      )
    ]
  )
  fun getPrisons(): List<PrisonDto> = prisonService.findAll()

  @GetMapping("/$PRISONS/search", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(summary = "Get prisons from active and text search", description = "All prisons")
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
  fun getPrisonsFromActiveAndTextSearch(
    @Parameter(description = "Active", example = "true", required = false) @RequestParam active: Boolean? = null,
    @Parameter(description = "Text search", example = "Sheffield", required = false) @RequestParam textSearch: String? = null
  ): List<PrisonDto> = prisonService.findByActiveAndTextSearch(active, textSearch)
}

@JsonInclude(NON_NULL)
@Schema(description = "Prison Information")
data class PrisonDto(
  @Schema(description = "Prison ID", example = "MDI", required = true) val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true) val prisonName: String,
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean,
  @Schema(description = "List of address for this prison") val addresses: List<AddressDto> = listOf()
) {
  constructor(prison: Prison) : this(
    prison.prisonId, prison.name, prison.active, prison.addresses.map { AddressDto(it) }
  )
}

data class AddressDto(
  @Schema(description = "Unique ID of the address", example = "10000", required = true) val id: Long,
  @Schema(description = "Address line 1", example = "Bawtry Road", required = false) val addressLine1: String?,
  @Schema(description = "Address line 2", example = "Hatfield Woodhouse", required = false) val addressLine2: String?,
  @Schema(description = "Village/Town/City", example = "Doncaster", required = true) val town: String,
  @Schema(description = "County", example = "South Yorkshire", required = false) val county: String?,
  @Schema(description = "Postcode", example = "DN7 6BW", required = true) val postcode: String,
  @Schema(description = "Country", example = "England", required = true) val country: String,

) {
  constructor(address: Address) : this(
    address.id!!, address.addressLine1, address.addressLine2, address.town, address.county, address.postcode, address.country
  )
}
