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
import uk.gov.justice.digital.hmpps.prisonregister.service.ApprovedPremiseService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_EMAIL_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_EMAIL_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_PHONE_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_PHONE_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.APPROVED_PREMISE_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/approved-premises", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class ApprovedPremiseResource(
  private val approvedPremiseService: ApprovedPremiseService,
  private val auditService: AuditService,
  private val snsService: SnsService,
) {
  @GetMapping("/id/{approvedPremiseId}")
  @Operation(summary = "Get specified approved premise", description = "Information on a specific approved premise")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getApprovedPremiseFromId(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
  ): ApprovedPremiseDto = approvedPremiseService.findById(approvedPremiseId)

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
  fun getApprovedPremises(): List<ApprovedPremiseDto> = approvedPremiseService.getAll()

  @Operation(
    summary = "Create a new approved premise",
    description = "Creates a approved premise, along with any addresses, email addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateApprovedPremiseDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Approved Premise Created",
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
        description = "Incorrect permissions to create a approved premise",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremise(
    @RequestBody @Valid
    createApprovedPremiseDto: CreateApprovedPremiseDto,
  ): ApprovedPremiseDto {
    val createdApprovedPremise = approvedPremiseService.createApprovedPremise(createApprovedPremiseDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterInsertedEvent(createApprovedPremiseDto.approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_INSERT.name,
      mapOf("approvedPremiseId" to createApprovedPremiseDto.approvedPremiseId, "approvedPremise" to createApprovedPremiseDto),
      now,
    )
    return createdApprovedPremise
  }

  @Operation(
    summary = "Update specified approved premise details",
    description = "Updates approved premise information, excluding its addresses, email addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateApprovedPremiseDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Approved Premise Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premise",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premise update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremiseId}")
  fun updateApprovedPremise(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @RequestBody @Valid
    updateApprovedPremiseDto: UpdateApprovedPremiseDto,
  ): ApprovedPremiseDto {
    val updatedApprovedPremise = approvedPremiseService.updateApprovedPremise(approvedPremiseId, updateApprovedPremiseDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_UPDATE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "approvedPremise" to updateApprovedPremiseDto),
      now,
    )
    return updatedApprovedPremise
  }

  @Operation(
    summary = "Delete specified approved premise",
    description = "Deletes a approved premise, along with any addresses, email addresses and phone numbers associated with it. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premise Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premise",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremiseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremise(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
  ) {
    approvedPremiseService.deleteApprovedPremise(approvedPremiseId)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterDeletedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_DELETE.name,
      mapOf("approvedPremiseId" to approvedPremiseId),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premise address",
    description = "Creates a new address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premise address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premise address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremiseId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremiseAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val createdAddress = approvedPremiseService.createApprovedPremiseAddress(approvedPremiseId, updateAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_ADDRESS_INSERT.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "address" to createdAddress),
      now,
    )
    return createdAddress
  }

  @Operation(
    summary = "Update specified approved premise address",
    description = "Updates a single address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premise address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premise address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremiseId}/address/{addressId}")
  fun updateApprovedPremiseAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = approvedPremiseService.updateApprovedPremiseAddress(approvedPremiseId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_ADDRESS_UPDATE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Delete specified approved premise address",
    description = "Deletes a single address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premise Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premise address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremiseId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremiseAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = approvedPremiseService.deleteApprovedPremiseAddress(approvedPremiseId, addressId)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_ADDRESS_DELETE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "address" to deletedAddress),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premise phone number",
    description = "Creates a new phone number for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Phone Number Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premise phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premise phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Phone number already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremiseId}/phone-number")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremisePhoneNumber(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val createdPhoneNumber = approvedPremiseService.createApprovedPremisePhoneNumber(approvedPremiseId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_PHONE_INSERT.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "phoneNumber" to createdPhoneNumber),
      now,
    )
    return createdPhoneNumber
  }

  @Operation(
    summary = "Update specified approved premise phone number",
    description = "Updates a single phone number for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premise phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premise phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremiseId}/phone-number/{phoneNumberId}")
  fun updateApprovedPremisePhoneNumber(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = approvedPremiseService.updateApprovedPremisePhoneNumber(approvedPremiseId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_PHONE_UPDATE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Delete specified approved premise phone number",
    description = "Deletes a single phone number for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premise Phone Number Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premise phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremiseId}/phone-number/{phoneNumberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremisePhoneNumber(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
  ) {
    val deletedPhoneNumber = approvedPremiseService.deleteApprovedPremisePhoneNumber(approvedPremiseId, phoneNumberId)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_PHONE_DELETE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "phoneNumber" to deletedPhoneNumber),
      now,
    )
  }

  @Operation(
    summary = "Create a approved premise email address",
    description = "Creates a new email address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Email Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create approved premise email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a approved premise email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Email address already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{approvedPremiseId}/email-address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createApprovedPremiseEmailAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val createdEmailAddress = approvedPremiseService.createApprovedPremiseEmailAddress(approvedPremiseId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_EMAIL_INSERT.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "emailAddress" to createdEmailAddress),
      now,
    )
    return createdEmailAddress
  }

  @Operation(
    summary = "Update specified approved premise email address",
    description = "Updates a single email address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Approved Premise Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update approved premise email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make approved premise email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{approvedPremiseId}/email-address/{emailAddressId}")
  fun updateApprovedPremiseEmailAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = approvedPremiseService.updateApprovedPremiseEmailAddress(approvedPremiseId, emailAddressId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_EMAIL_UPDATE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "emailAddress" to updatedEmailAddress),
      now,
    )
    return updatedEmailAddress
  }

  @Operation(
    summary = "Delete specified approved premise email address",
    description = "Deletes a single email address for a approved premise. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Approved Premise Email Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a approved premise email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Approved Premise Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{approvedPremiseId}/email-address/{emailAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteApprovedPremiseEmailAddress(
    @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 letters")
    approvedPremiseId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
  ) {
    val deletedEmailAddress = approvedPremiseService.deleteApprovedPremiseEmailAddress(approvedPremiseId, emailAddressId)
    val now = Instant.now()
    snsService.sendApprovedPremiseRegisterAmendedEvent(approvedPremiseId, now)
    auditService.sendAuditEvent(
      APPROVED_PREMISE_REGISTER_EMAIL_DELETE.name,
      mapOf("approvedPremiseId" to approvedPremiseId, "emailAddress" to deletedEmailAddress),
      now,
    )
  }
}

@Schema(description = "Approved Premise Information")
@JsonInclude(NON_NULL)
data class ApprovedPremiseDto(
  @Schema(description = "Approved Premise ID", example = "SHEFAP") val approvedPremiseId: String,
  @Schema(description = "Name", example = "Sheffield Approved Premise") val approvedPremiseName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premise") val description: String?,
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

@Schema(description = "Approved Premise Update Record")
@JsonInclude(NON_NULL)
data class UpdateApprovedPremiseDto(
  @Schema(description = "Name", example = "Sheffield Approved Premise", required = true)
  @field:NotBlank(message = "Approved Premise name is required")
  @field:Size(max = 40, message = "Approved Premise name must be no more than 40 characters")
  val approvedPremiseName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premise")
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

@Schema(description = "Approved Premise Create Record")
@JsonInclude(NON_NULL)
data class CreateApprovedPremiseDto(
  @Schema(description = "Approved Premise ID", example = "SHEFAP", required = true)
  @field:NotBlank(message = "Approved Premise id is required")
  @field:Size(min = 2, max = 6, message = "Approved Premise Id must be between 2 and 6 characters")
  val approvedPremiseId: String,
  @Schema(description = "Name", example = "Sheffield Approved Premise", required = true)
  @field:NotBlank(message = "Approved Premise name is required")
  @field:Size(max = 40, message = "Approved Premise name must be no more than 40 characters")
  val approvedPremiseName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Approved Premise")
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
