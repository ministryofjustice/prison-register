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
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.Address
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.Operator
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.model.PrisonRequest
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonAddressService
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

@RestController
@Validated
@RequestMapping("/prisons", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonResource(private val prisonService: PrisonService, private val addressService: PrisonAddressService) {
  @GetMapping("/id/{prisonId}")
  @Operation(summary = "Get specified prison", description = "Information on a specific prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PrisonDto::class))],
      ),
    ],
  )
  fun getPrisonFromId(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 letters")
    prisonId: String,
  ): PrisonDto = prisonService.findById(prisonId)

  @GetMapping
  @Operation(summary = "Get all prisons", description = "All prisons")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonDto::class)),
          ),
        ],
      ),
    ],
  )
  fun getPrisons(): List<PrisonDto> = prisonService.findAll()

  @GetMapping("/id/{prisonId}/address/{addressId}")
  @Operation(summary = "Get specified prison", description = "Information on a specific prison address")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getAddressFromId(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 letters")
    prisonId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ): AddressDto =
    addressService.findById(prisonId, addressId)

  @GetMapping("/search")
  @Operation(summary = "Get prisons from active and text search", description = "All prisons")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getPrisonsBySearchFilter(
    @Parameter(description = "Active", example = "true", required = false) @RequestParam active: Boolean? = null,
    @Parameter(
      description = "Text search",
      example = "Sheffield",
      required = false,
    ) @RequestParam textSearch: String? = null,
    @Parameter(
      description = "Genders to filter by",
      example = "MALE, FEMALE",
      required = false,
    ) @RequestParam genders: List<Gender>? = listOf(),
    @Parameter(
      description = "Prison type codes to filter by",
      example = "HMP, YOI",
      required = false,
    ) @RequestParam prisonTypeCodes: List<Type>? = listOf(),
  ): List<PrisonDto> = prisonService.findByPrisonFilter(active, textSearch, genders, prisonTypeCodes)

  @GetMapping(
    "/names",
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(summary = "Get prison names", description = "prison id and full name")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getPrisonNames(): List<PrisonNameDto> {
    return prisonService.getPrisonNames()
  }

  @PostMapping("/prisonsByIds", consumes = ["application/json"])
  @Operation(summary = "Get prisons by IDs", description = "Get prisons based on their IDs")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful operation",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = PrisonDto::class)),
          ),
        ],
      ),
    ],
  )
  fun getPrisonsByIds(
    @RequestBody @Valid prisonRequest: PrisonRequest,
  ) = prisonService.findPrisonsByIds(prisonRequest.prisonIds)
}

@JsonInclude(NON_NULL)
@Schema(description = "Prison Information")
data class PrisonDto(
  @Schema(description = "Prison ID", example = "MDI", required = true) val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true) val prisonName: String,
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean,
  @Schema(description = "Whether the prison has male prisoners") val male: Boolean,
  @Schema(description = "Whether the prison has female prisoners") val female: Boolean,
  @Schema(description = "Whether the prison is contracted") val contracted: Boolean,
  @Schema(description = "Whether the prison is part of long term high security estate") val lthse: Boolean,
  @Schema(description = "List of types for this prison") val types: List<PrisonTypeDto> = listOf(),
  @Schema(description = "List of the categories for this prison") val categories: Set<Category> = setOf(),
  @Schema(description = "List of address for this prison") val addresses: List<AddressDto> = listOf(),
  @Schema(description = "List of operators for this prison") val operators: List<PrisonOperatorDto> = listOf(),
) {
  constructor(prison: Prison) : this(
    prison.prisonId,
    prison.name,
    prison.active,
    prison.male,
    prison.female,
    prison.contracted,
    prison.lthse,
    prison.prisonTypes.map { PrisonTypeDto(it) },
    prison.categories.also { Hibernate.initialize(it) },
    prison.addresses.map { AddressDto(it) },
    prison.prisonOperators.map { PrisonOperatorDto(it) },
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
    address.id!!,
    address.addressLine1,
    address.addressLine2,
    address.town,
    address.county,
    address.postcode,
    address.country,
  )
}

data class PrisonTypeDto(
  @Schema(description = "Prison type code", example = "HMP", required = true) val code: Type,
  @Schema(
    description = "Prison type description",
    example = "His Majesty’s Prison",
    required = true,
  ) val description: String,
) {
  constructor(prisonType: PrisonType) : this(prisonType.type, prisonType.type.description)
}

data class PrisonOperatorDto(
  @Schema(description = "Prison operator name", example = "PSP, G4S", required = true) val name: String,
) {
  constructor(prisonOperator: Operator) : this(prisonOperator.name)
}
