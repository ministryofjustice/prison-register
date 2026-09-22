package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.ApprovedPremisesService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_EMAIL_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_EMAIL_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_PHONE_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_PHONE_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISES_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/approved-premises", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class ApprovedPremisesResource(
  private val approvedPremisesService: ApprovedPremisesService,
  private val auditService: AuditService,
  private val snsService: SnsService,
) {
  @GetMapping("/id/{approvedPremisesId}")
  @Operation(summary = "Get specified approved premises", description = "Information on a specific approved premises")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getApprovedPremisesFromId(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
  ): ApprovedPremisesDto = approvedPremisesService.findById(approvedPremisesId)

  @GetMapping
  @Operation(summary = "Get all approved premises", description = "Information on all approved premises")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getApprovedPremises(): List<ApprovedPremisesDto> = approvedPremisesService.getAll()

  @Operation(
    summary = "Create a new approved premises",
    description = "Creates a approved premises, along with any addresses, email addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateApprovedPremisesDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Approved Premises Created",
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
        description = "Incorrect permissions to create a approved premises",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremises(
    @RequestBody @Valid
    createApprovedPremisesDto: CreateApprovedPremisesDto,
  ): ApprovedPremisesDto {
    val createdApprovedPremises = approvedPremisesService.createApprovedPremises(createApprovedPremisesDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterInsertedEvent(createApprovedPremisesDto.approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_INSERT.name,
      mapOf("approvedPremisesId" to createApprovedPremisesDto.approvedPremisesId, "approvedPremises" to createApprovedPremisesDto),
      now,
    )
    return createdApprovedPremises
  }

  @Operation(
    summary = "Update specified approved premises details",
    description = "Updates approved premises information, excluding its addresses, email addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateApprovedPremisesDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Approved Premises Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premises",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premises update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremisesId}")
  fun updateApprovedPremises(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @RequestBody @Valid
    updateApprovedPremisesDto: UpdateApprovedPremisesDto,
  ): ApprovedPremisesDto {
    val updatedApprovedPremises = approvedPremisesService.updateApprovedPremises(approvedPremisesId, updateApprovedPremisesDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_UPDATE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "approvedPremises" to updateApprovedPremisesDto),
      now,
    )
    return updatedApprovedPremises
  }

  @Operation(
    summary = "Delete specified approved premises",
    description = "Deletes a approved premises, along with any addresses, email addresses and phone numbers associated with it. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premises Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premises",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremisesId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremises(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
  ) {
    approvedPremisesService.deleteApprovedPremises(approvedPremisesId)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterDeletedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_DELETE.name,
      mapOf("approvedPremisesId" to approvedPremisesId),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premises address",
    description = "Creates a new address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premises address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premises address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremisesId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremisesAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val createdAddress = approvedPremisesService.createApprovedPremisesAddress(approvedPremisesId, updateAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_ADDRESS_INSERT.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "address" to createdAddress),
      now,
    )
    return createdAddress
  }

  @Operation(
    summary = "Update specified approved premises address",
    description = "Updates a single address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premises address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premises address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremisesId}/address/{addressId}")
  fun updateApprovedPremisesAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = approvedPremisesService.updateApprovedPremisesAddress(approvedPremisesId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_ADDRESS_UPDATE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Delete specified approved premises address",
    description = "Deletes a single address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premises Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premises address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremisesId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremisesAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = approvedPremisesService.deleteApprovedPremisesAddress(approvedPremisesId, addressId)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_ADDRESS_DELETE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "address" to deletedAddress),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premises phone number",
    description = "Creates a new phone number for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Phone Number Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premises phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premises phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Phone number already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremisesId}/phone-number")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremisesPhoneNumber(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val createdPhoneNumber = approvedPremisesService.createApprovedPremisesPhoneNumber(approvedPremisesId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_PHONE_INSERT.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "phoneNumber" to createdPhoneNumber),
      now,
    )
    return createdPhoneNumber
  }

  @Operation(
    summary = "Update specified approved premises phone number",
    description = "Updates a single phone number for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premises phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premises phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremisesId}/phone-number/{phoneNumberId}")
  fun updateApprovedPremisesPhoneNumber(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = approvedPremisesService.updateApprovedPremisesPhoneNumber(approvedPremisesId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_PHONE_UPDATE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Delete specified approved premises phone number",
    description = "Deletes a single phone number for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premises Phone Number Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premises phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremisesId}/phone-number/{phoneNumberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremisesPhoneNumber(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
  ) {
    val deletedPhoneNumber = approvedPremisesService.deleteApprovedPremisesPhoneNumber(approvedPremisesId, phoneNumberId)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_PHONE_DELETE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "phoneNumber" to deletedPhoneNumber),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premises email address",
    description = "Creates a new email address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Email Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premises email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premises email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Email address already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremisesId}/email-address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremisesEmailAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val createdEmailAddress = approvedPremisesService.createApprovedPremisesEmailAddress(approvedPremisesId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_EMAIL_INSERT.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "emailAddress" to createdEmailAddress),
      now,
    )
    return createdEmailAddress
  }

  @Operation(
    summary = "Update specified approved premises email address",
    description = "Updates a single email address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premises Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premises email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premises email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremisesId}/email-address/{emailAddressId}")
  fun updateApprovedPremisesEmailAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = approvedPremisesService.updateApprovedPremisesEmailAddress(approvedPremisesId, emailAddressId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_EMAIL_UPDATE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "emailAddress" to updatedEmailAddress),
      now,
    )
    return updatedEmailAddress
  }

  @Operation(
    summary = "Delete specified approved premises email address",
    description = "Deletes a single email address for a approved premises. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premises Email Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premises email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premises Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremisesId}/email-address/{emailAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremisesEmailAddress(
    @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 letters")
    approvedPremisesId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
  ) {
    val deletedEmailAddress = approvedPremisesService.deleteApprovedPremisesEmailAddress(approvedPremisesId, emailAddressId)
    val now = Instant.now()
    snsService.sendApprovedPremisesRegisterAmendedEvent(approvedPremisesId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISES_REGISTER_EMAIL_DELETE.name,
      mapOf("approvedPremisesId" to approvedPremisesId, "emailAddress" to deletedEmailAddress),
      now,
    )
  }
}

@Schema(description = "Approved Premises Information")
@JsonInclude(NON_NULL)
data class ApprovedPremisesDto(
  @Schema(description = "Approved Premises ID", example = "SHEFAP") val approvedPremisesId: String,
  @Schema(description = "Name", example = "Sheffield Approved Premises") val approvedPremisesName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premises") val description: String?,
  @Schema(description = "Contact", example = "John Smith") val contact: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE") val accessibleAccess: String?,
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

@Schema(description = "Approved Premises Update Record")
@JsonInclude(NON_NULL)
data class UpdateApprovedPremisesDto(
  @Schema(description = "Name", example = "Sheffield Approved Premises", required = true)
  @field:NotBlank(message = "Approved Premises name is required")
  @field:Size(max = 40, message = "Approved Premises name must be no more than 40 characters")
  val approvedPremisesName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premises")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Contact", example = "John Smith")
  @field:Size(max = 40, message = "Contact must be no more than 40 characters")
  val contact: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: AccessibleAccess?,
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

@Schema(description = "Approved Premises Create Record")
@JsonInclude(NON_NULL)
data class CreateApprovedPremisesDto(
  @Schema(description = "Approved Premises ID", example = "SHEFAP", required = true)
  @field:NotBlank(message = "Approved Premises id is required")
  @field:Size(min = 2, max = 6, message = "Approved Premises Id must be between 2 and 6 characters")
  val approvedPremisesId: String,
  @Schema(description = "Name", example = "Sheffield Approved Premises", required = true)
  @field:NotBlank(message = "Approved Premises name is required")
  @field:Size(max = 40, message = "Approved Premises name must be no more than 40 characters")
  val approvedPremisesName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premises")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Contact", example = "John Smith")
  @field:Size(max = 40, message = "Contact must be no more than 40 characters")
  val contact: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean = true,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: AccessibleAccess?,
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
