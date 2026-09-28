package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.model.OtherAgencyType
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_EMAIL_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_EMAIL_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_PHONE_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_PHONE_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.AGENCY_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.OtherAgencyService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/other-agencies", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('HMPPS_REGISTERS_API__MAINTAIN__RW')")
class OtherAgencyResource(
  private val otherAgencyService: OtherAgencyService,
  private val auditService: AuditService,
  private val snsService: SnsService,
) {
  @GetMapping("/id/{agencyId}")
  @Operation(summary = "Get specified agency", description = "Information on a specific agency")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getOtherAgencyFromId(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
  ): OtherAgencyDto = otherAgencyService.findById(agencyId)

  @GetMapping
  @Operation(summary = "Get all agencies", description = "Information on all agencies")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getOtherAgencies(
    @Parameter(description = "Active", example = "true", required = false) @RequestParam active: Boolean? = null,
    @Parameter(description = "Text search", example = "Sheffield", required = false) @RequestParam textSearch: String? = null,
    @Parameter(description = "Agency type codes to filter by", example = "AIRPORT, PECS", required = false) @RequestParam otherAgencyTypeCodes: List<OtherAgencyType>? = listOf(),
  ): List<OtherAgencyDto> = otherAgencyService.getAll(
    active = active,
    textSearch = textSearch,
    otherAgencyTypeCodes = otherAgencyTypeCodes,
  )

  @Operation(
    summary = "Create a new agency",
    description = "Creates a agency, along with any addresses, email addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateOtherAgencyDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a agency",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createOtherAgency(
    @RequestBody @Valid
    createAgencyDto: CreateOtherAgencyDto,
  ): OtherAgencyDto {
    val createdAgency = otherAgencyService.createAgency(createAgencyDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterInsertedEvent(createAgencyDto.agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_INSERT.name,
      mapOf("agencyId" to createAgencyDto.agencyId, "agency" to createAgencyDto),
      now,
    )
    return createdAgency
  }

  @Operation(
    summary = "Update specified agency details",
    description = "Updates agency information, excluding its addresses, email addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateOtherAgencyDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update agency",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make agency update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{agencyId}")
  fun updateOtherAgency(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @RequestBody @Valid
    updateAgencyDto: UpdateOtherAgencyDto,
  ): OtherAgencyDto {
    val updatedAgency = otherAgencyService.updateAgency(agencyId, updateAgencyDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_UPDATE.name,
      mapOf("agencyId" to agencyId, "agency" to updateAgencyDto),
      now,
    )
    return updatedAgency
  }

  @Operation(
    summary = "Delete specified agency",
    description = "Deletes a agency, along with any addresses, email addresses and phone numbers associated with it. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Agency Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a agency",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{agencyId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteOtherAgency(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
  ) {
    otherAgencyService.deleteAgency(agencyId)
    val now = Instant.now()
    snsService.sendAgencyRegisterDeletedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_DELETE.name,
      mapOf("agencyId" to agencyId),
      now,
    )
  }

  @Operation(
    summary = "Create a agency address",
    description = "Creates a new address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        responseCode = "201",
        description = "Agency Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create agency address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a agency address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{agencyId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createOtherAgencyAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val createdAddress = otherAgencyService.createAgencyAddress(agencyId, updateAddressDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_ADDRESS_INSERT.name,
      mapOf("agencyId" to agencyId, "address" to createdAddress),
      now,
    )
    return createdAddress
  }

  @Operation(
    summary = "Update specified agency address",
    description = "Updates a single address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Agency Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update agency address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make agency address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{agencyId}/address/{addressId}")
  fun updateOtherAgencyAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = otherAgencyService.updateAgencyAddress(agencyId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_ADDRESS_UPDATE.name,
      mapOf("agencyId" to agencyId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Delete specified agency address",
    description = "Deletes a single address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Agency Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a agency address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{agencyId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteOtherAgencyAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = otherAgencyService.deleteAgencyAddress(agencyId, addressId)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_ADDRESS_DELETE.name,
      mapOf("agencyId" to agencyId, "address" to deletedAddress),
      now,
    )
  }

  @Operation(
    summary = "Create a agency phone number",
    description = "Creates a new phone number for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdatePhoneNumberDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency Phone Number Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create agency phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a agency phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Phone number already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{agencyId}/phone-number")
  @ResponseStatus(HttpStatus.CREATED)
  fun createOtherAgencyPhoneNumber(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val createdPhoneNumber = otherAgencyService.createAgencyPhoneNumber(agencyId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_PHONE_INSERT.name,
      mapOf("agencyId" to agencyId, "phoneNumber" to createdPhoneNumber),
      now,
    )
    return createdPhoneNumber
  }

  @Operation(
    summary = "Update specified agency phone number",
    description = "Updates a single phone number for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdatePhoneNumberDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update agency phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make agency phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{agencyId}/phone-number/{phoneNumberId}")
  fun updateOtherAgencyPhoneNumber(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = otherAgencyService.updateAgencyPhoneNumber(agencyId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_PHONE_UPDATE.name,
      mapOf("agencyId" to agencyId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Delete specified agency phone number",
    description = "Deletes a single phone number for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Agency Phone Number Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a agency phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{agencyId}/phone-number/{phoneNumberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteOtherAgencyPhoneNumber(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
  ) {
    val deletedPhoneNumber = otherAgencyService.deleteAgencyPhoneNumber(agencyId, phoneNumberId)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_PHONE_DELETE.name,
      mapOf("agencyId" to agencyId, "phoneNumber" to deletedPhoneNumber),
      now,
    )
  }

  @Operation(
    summary = "Create a agency email address",
    description = "Creates a new email address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateEmailAddressDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency Email Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create agency email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a agency email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Email address already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{agencyId}/email-address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createOtherAgencyEmailAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val createdEmailAddress = otherAgencyService.createAgencyEmailAddress(agencyId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_EMAIL_INSERT.name,
      mapOf("agencyId" to agencyId, "emailAddress" to createdEmailAddress),
      now,
    )
    return createdEmailAddress
  }

  @Operation(
    summary = "Update specified agency email address",
    description = "Updates a single email address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateEmailAddressDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update agency email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make other agency email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Other Agency Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{agencyId}/email-address/{emailAddressId}")
  fun updateOtherAgencyEmailAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = otherAgencyService.updateAgencyEmailAddress(agencyId, emailAddressId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_EMAIL_UPDATE.name,
      mapOf("agencyId" to agencyId, "emailAddress" to updatedEmailAddress),
      now,
    )
    return updatedEmailAddress
  }

  @Operation(
    summary = "Delete specified agency email address",
    description = "Deletes a single email address for a agency. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Agency Email Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a agency email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{agencyId}/email-address/{emailAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteOtherAgencyEmailAddress(
    @Schema(description = "Agency ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Agency Id must be between 2 and 6 letters")
    agencyId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
  ) {
    val deletedEmailAddress = otherAgencyService.deleteAgencyEmailAddress(agencyId, emailAddressId)
    val now = Instant.now()
    snsService.sendAgencyRegisterAmendedEvent(agencyId, now)
    auditService.sendAuditEvent(
      AGENCY_REGISTER_EMAIL_DELETE.name,
      mapOf("agencyId" to agencyId, "emailAddress" to deletedEmailAddress),
      now,
    )
  }
}

@Schema(description = "Other Agency Information")
@JsonInclude(NON_NULL)
data class OtherAgencyDto(
  @Schema(description = "Agency ID", example = "SHEFCC") val agencyId: String,
  @Schema(description = "Name", example = "Sheffield Agency") val agencyName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Agency") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE") val accessibleAccess: String?,
  @Schema(description = "Agency type", example = "PROBATION_CRC") val agencyType: String,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographical Area") val geographicalArea: CodeDescription?,
  @Schema(description = "Local Authority") val localAuthority: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)

@Schema(description = "Other Agency Update Record")
@JsonInclude(NON_NULL)
data class UpdateOtherAgencyDto(
  @Schema(description = "Name", example = "Sheffield Agency", required = true)
  @field:NotBlank(message = "Agency name is required")
  @field:Size(max = 40, message = "Agency name must be no more than 40 characters")
  val agencyName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Agency")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: AccessibleAccess?,
  @Schema(description = "Agency type", example = "PROBATION_CRC", required = true)
  val otherAgencyType: OtherAgencyType,
  @Schema(description = "Date made inactive", example = "2023-12-31")
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789")
  @field:Size(max = 12, message = "CJIT code must be no more than 12 characters")
  val cjitCode: String?,
  @Schema(description = "Area code", example = "52")
  @field:Size(max = 12, message = "Area code must be no more than 12 characters")
  val areaCode: String?,
  @Schema(description = "Region code", example = "YOHUM")
  @field:Size(max = 12, message = "Region code must be no more than 12 characters")
  val regionCode: String?,
  @Schema(description = "Geographical Area code", example = "WYORKS")
  @field:Size(max = 12, message = "Geographical area code must be no more than 12 characters")
  val geographicalAreaCode: String?,
  @Schema(description = "Local Authority code", example = "00CG")
  val localAuthorityCode: String?,
  @Schema(description = "Prisoner Payroll Region code", example = "NEY")
  val payrollRegionCode: String?,
)

@Schema(description = "Other Agency Create Record")
@JsonInclude(NON_NULL)
data class CreateOtherAgencyDto(
  @Schema(description = "Other Agency ID", example = "SHEFCC", required = true)
  @field:NotBlank(message = "Other Agency id is required")
  @field:Size(min = 2, max = 6, message = "Other Agency Id must be between 2 and 6 characters")
  val agencyId: String,
  @Schema(description = "Name", example = "Sheffield Agency", required = true)
  @field:NotBlank(message = "Agency name is required")
  @field:Size(max = 40, message = "Agency name must be no more than 40 characters")
  val agencyName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Agency")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean = true,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: AccessibleAccess?,
  @Schema(description = "Agency type", example = "PROBATION_CRC", required = true)
  val otherAgencyType: OtherAgencyType,
  @Schema(description = "Date made inactive", example = "2023-12-31")
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789")
  @field:Size(max = 12, message = "CJIT code must be no more than 12 characters")
  val cjitCode: String?,
  @Schema(description = "Area code", example = "52")
  @field:Size(max = 12, message = "Area code must be no more than 12 characters")
  val areaCode: String?,
  @Schema(description = "Region code", example = "YOHUM")
  @field:Size(max = 12, message = "Region code must be no more than 12 characters")
  val regionCode: String?,
  @Schema(description = "Geographical Area code", example = "WYORKS")
  @field:Size(max = 12, message = "Geographical area code must be no more than 12 characters")
  val geographicalAreaCode: String?,
  @Schema(description = "Local Authority code", example = "00CG")
  val localAuthorityCode: String?,
  @Schema(description = "Prisoner Payroll Region code", example = "NEY")
  val payrollRegionCode: String?,
  @Schema(description = "Addresses")
  @field:Valid
  val addresses: List<UpdateAddressDto> = listOf(),
  @Schema(description = "Email addresses")
  @field:Valid
  val emailAddresses: List<UpdateEmailAddressDto> = listOf(),
  @Schema(description = "Phone numbers")
  @field:Valid
  val phoneNumbers: List<UpdatePhoneNumberDto> = listOf(),
)
