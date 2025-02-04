package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PRISON_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PRISON_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PRISON_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.CLIENT_CAN_MAINTAIN_PRISON_DETAILS
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonAddressService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant

@RestController
@Validated
@RequestMapping(name = "Prison Maintenance", path = ["/prison-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonAddressMaintenanceResource(
  private val addressService: PrisonAddressService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  @Operation(
    summary = "Update specified address details",
    description = "Updates address information, role required is MAINTAIN_REF_DATA or MAINTAIN_PRISON_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"]), SecurityRequirement(name = "MAINTAIN_PRISON_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateAddressDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Information request to update address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{prisonId}/address/{addressId}")
  fun updateAddress(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 characters")
    prisonId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AddressDto {
    val updatedAddress = addressService.updateAddress(prisonId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendPrisonRegisterAmendedEvent(prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_ADDRESS_UPDATE.name,
      mapOf("prisonId" to prisonId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @PutMapping("/id/{prisonId}/welsh-address/{addressId}")
  fun updateWelshAddress(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 characters")
    prisonId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateWelshAddressDto: UpdateWelshAddressDto,
  ): AddressDto {
    val updatedAddress = addressService.updateWelshAddress(prisonId, addressId, updateWelshAddressDto)
    val now = Instant.now()
    snsService.sendPrisonRegisterAmendedEvent(prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_ADDRESS_UPDATE.name,
      mapOf("prisonId" to prisonId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  @Operation(
    summary = "Delete specified address for specified Prison",
    description = "Deletes address information for a Prison, role required is MAINTAIN_REF_DATA or MAINTAIN_PRISON_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"]), SecurityRequirement(name = "MAINTAIN_PRISON_DATA", scopes = ["write"])],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Address Information Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{prisonId}/address/{addressId}")
  fun deleteAddress(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 characters")
    prisonId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = addressService.deleteAddress(prisonId, addressId)
    val now = Instant.now()
    snsService.sendPrisonRegisterAmendedEvent(prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_ADDRESS_DELETE.name,
      mapOf("prisonId" to prisonId, "address" to deletedAddress),
      now,
    )
  }

  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  @Operation(
    summary = "Add Address to existing Prison",
    description = "Adds an additional Address to an existing Prison, role required is MAINTAIN_REF_DATA or MAINTAIN_PRISON_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"]), SecurityRequirement(name = "MAINTAIN_PRISON_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateAddressDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "New Address added to Prison",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Information request to update address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add Prison address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{prisonId}/address")
  fun addAddress(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 characters")
    prisonId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AddressDto {
    val additionalAddress = addressService.addAddress(prisonId, updateAddressDto)
    val now = Instant.now()
    snsService.sendPrisonRegisterAmendedEvent(prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_ADDRESS_INSERT.name,
      mapOf("prisonId" to prisonId, "address" to additionalAddress),
      now,
    )

    return additionalAddress
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Address Update Record")
data class UpdateAddressDto(
  @Schema(description = "Address line 1", example = "Bawtry Road")
  @field:Size(
    max = 80,
    message = "Address line 1 must be no more than 80 characters",
  )
  val addressLine1: String?,
  @Schema(description = "Address line 2", example = "Hatfield Woodhouse")
  @field:Size(
    max = 80,
    message = "Address line 2 must be no more than 80 characters",
  )
  val addressLine2: String?,
  @Schema(description = "Village/Town/City", example = "Doncaster", required = true)
  @field:Size(
    max = 80,
    message = "Village/Town/City must be no more than 80 characters",
  )
  val town: String,
  @Schema(description = "County", example = "South Yorkshire")
  @field:Size(
    max = 80,
    message = "County must be no more than 80 characters",
  )
  val county: String?,
  @Schema(description = "Postcode", example = "DN7 6BW", required = true)
  @field:Size(
    max = 8,
    message = "Postcode must be no more than 8 characters",
  )
  val postcode: String,
  @Schema(description = "Country", example = "England", required = true)
  @field:Size(
    max = 16,
    message = "Country must be no more than 16 characters",
  )
  val country: String,
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Welsh Address Update Record")
data class UpdateWelshAddressDto(

  @Schema(description = "Address line 1 in Welsh", example = "Bawtry Road", required = false)
  @field:Size(
    max = 80,
    message = "Address line 1 must be no more than 80 characters",
  )
  val addressLine1InWelsh: String?,

  @Schema(description = "Address line 2 in Welsh", example = "Hatfield Woodhouse", required = false)
  @field:Size(
    max = 80,
    message = "Address line 2 must be no more than 80 characters",
  )
  val addressLine2InWelsh: String?,

  @Schema(description = "Village/Town/City in Welsh", example = "Brynbuga", required = false)
  @field:Size(
    max = 80,
    message = "Village/Town/City must be no more than 80 characters",
  )
  val townInWelsh: String?,

  @Schema(description = "County in Welsh", example = "Sir Fynwy", required = false)
  @field:Size(
    max = 80,
    message = "County must be no more than 80 characters",
  )
  val countyInWelsh: String?,

  @Schema(description = "Country in Welsh", example = "Cymru", required = false)
  @field:Size(
    max = 16,
    message = "Country must be no more than 16 characters",
  )
  val countryInWelsh: String?,
)