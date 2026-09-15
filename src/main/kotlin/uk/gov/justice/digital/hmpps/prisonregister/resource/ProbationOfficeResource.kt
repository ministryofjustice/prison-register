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
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_EMAIL_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_EMAIL_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_PHONE_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_PHONE_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PROBATION_OFFICE_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.ProbationOfficeService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/probation-offices", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class ProbationOfficeResource(
  private val probationOfficeService: ProbationOfficeService,
  private val auditService: AuditService,
  private val snsService: SnsService,
) {
  @GetMapping("/id/{probationOfficeId}")
  @Operation(summary = "Get specified probation office", description = "Information on a specific probation office")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getProbationOfficeFromId(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
  ): ProbationOfficeDto = probationOfficeService.findById(probationOfficeId)

  @GetMapping
  @Operation(summary = "Get all probation offices", description = "Information on all probation offices")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getProbationOffices(): List<ProbationOfficeDto> = probationOfficeService.getAll()

  @Operation(
    summary = "Create a new probation office",
    description = "Creates a probation office, along with any addresses, email addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateProbationOfficeDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Probation Office Created",
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
        description = "Incorrect permissions to create a probation office",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createProbationOffice(
    @RequestBody @Valid
    createProbationOfficeDto: CreateProbationOfficeDto,
  ): ProbationOfficeDto {
    val createdProbationOffice = probationOfficeService.createProbationOffice(createProbationOfficeDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterInsertedEvent(createProbationOfficeDto.probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_INSERT.name,
      mapOf("probationOfficeId" to createProbationOfficeDto.probationOfficeId, "probationOffice" to createProbationOfficeDto),
      now,
    )
    return createdProbationOffice
  }

  @Operation(
    summary = "Update specified probation office details",
    description = "Updates probation office information, excluding its addresses, email addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateProbationOfficeDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Probation Office Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update probation office",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make probation office update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{probationOfficeId}")
  fun updateProbationOffice(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @RequestBody @Valid
    updateProbationOfficeDto: UpdateProbationOfficeDto,
  ): ProbationOfficeDto {
    val updatedProbationOffice = probationOfficeService.updateProbationOffice(probationOfficeId, updateProbationOfficeDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_UPDATE.name,
      mapOf("probationOfficeId" to probationOfficeId, "probationOffice" to updateProbationOfficeDto),
      now,
    )
    return updatedProbationOffice
  }

  @Operation(
    summary = "Delete specified probation office",
    description = "Deletes a probation office, along with any addresses, email addresses and phone numbers associated with it. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Probation Office Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a probation office",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{probationOfficeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProbationOffice(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
  ) {
    probationOfficeService.deleteProbationOffice(probationOfficeId)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterDeletedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_DELETE.name,
      mapOf("probationOfficeId" to probationOfficeId),
      now,
    )
  }

  @Operation(
    summary = "Create a probation office address",
    description = "Creates a new address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create probation office address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a probation office address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{probationOfficeId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createProbationOfficeAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val createdAddress = probationOfficeService.createProbationOfficeAddress(probationOfficeId, updateAddressDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_ADDRESS_INSERT.name,
      mapOf("probationOfficeId" to probationOfficeId, "address" to createdAddress),
      now,
    )
    return createdAddress
  }

  @Operation(
    summary = "Update specified probation office address",
    description = "Updates a single address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update probation office address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make probation office address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{probationOfficeId}/address/{addressId}")
  fun updateProbationOfficeAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = probationOfficeService.updateProbationOfficeAddress(probationOfficeId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_ADDRESS_UPDATE.name,
      mapOf("probationOfficeId" to probationOfficeId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Delete specified probation office address",
    description = "Deletes a single address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Probation Office Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a probation office address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{probationOfficeId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProbationOfficeAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = probationOfficeService.deleteProbationOfficeAddress(probationOfficeId, addressId)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_ADDRESS_DELETE.name,
      mapOf("probationOfficeId" to probationOfficeId, "address" to deletedAddress),
      now,
    )
  }

  @Operation(
    summary = "Create a probation office phone number",
    description = "Creates a new phone number for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Phone Number Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create probation office phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a probation office phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Phone number already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{probationOfficeId}/phone-number")
  @ResponseStatus(HttpStatus.CREATED)
  fun createProbationOfficePhoneNumber(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val createdPhoneNumber = probationOfficeService.createProbationOfficePhoneNumber(probationOfficeId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_PHONE_INSERT.name,
      mapOf("probationOfficeId" to probationOfficeId, "phoneNumber" to createdPhoneNumber),
      now,
    )
    return createdPhoneNumber
  }

  @Operation(
    summary = "Update specified probation office phone number",
    description = "Updates a single phone number for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update probation office phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make probation office phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{probationOfficeId}/phone-number/{phoneNumberId}")
  fun updateProbationOfficePhoneNumber(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = probationOfficeService.updateProbationOfficePhoneNumber(probationOfficeId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_PHONE_UPDATE.name,
      mapOf("probationOfficeId" to probationOfficeId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Delete specified probation office phone number",
    description = "Deletes a single phone number for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Probation Office Phone Number Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a probation office phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{probationOfficeId}/phone-number/{phoneNumberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProbationOfficePhoneNumber(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
  ) {
    val deletedPhoneNumber = probationOfficeService.deleteProbationOfficePhoneNumber(probationOfficeId, phoneNumberId)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_PHONE_DELETE.name,
      mapOf("probationOfficeId" to probationOfficeId, "phoneNumber" to deletedPhoneNumber),
      now,
    )
  }

  @Operation(
    summary = "Create a probation office email address",
    description = "Creates a new email address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Email Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create probation office email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a probation office email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Email address already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{probationOfficeId}/email-address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createProbationOfficeEmailAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val createdEmailAddress = probationOfficeService.createProbationOfficeEmailAddress(probationOfficeId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_EMAIL_INSERT.name,
      mapOf("probationOfficeId" to probationOfficeId, "emailAddress" to createdEmailAddress),
      now,
    )
    return createdEmailAddress
  }

  @Operation(
    summary = "Update specified probation office email address",
    description = "Updates a single email address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Probation Office Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update probation office email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make probation office email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{probationOfficeId}/email-address/{emailAddressId}")
  fun updateProbationOfficeEmailAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = probationOfficeService.updateProbationOfficeEmailAddress(probationOfficeId, emailAddressId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_EMAIL_UPDATE.name,
      mapOf("probationOfficeId" to probationOfficeId, "emailAddress" to updatedEmailAddress),
      now,
    )
    return updatedEmailAddress
  }

  @Operation(
    summary = "Delete specified probation office email address",
    description = "Deletes a single email address for a probation office. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Probation Office Email Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a probation office email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Probation Office Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{probationOfficeId}/email-address/{emailAddressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteProbationOfficeEmailAddress(
    @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 letters")
    probationOfficeId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
  ) {
    val deletedEmailAddress = probationOfficeService.deleteProbationOfficeEmailAddress(probationOfficeId, emailAddressId)
    val now = Instant.now()
    snsService.sendProbationOfficeRegisterAmendedEvent(probationOfficeId, now)
    auditService.sendAuditEvent(
      PROBATION_OFFICE_REGISTER_EMAIL_DELETE.name,
      mapOf("probationOfficeId" to probationOfficeId, "emailAddress" to deletedEmailAddress),
      now,
    )
  }
}

@Schema(description = "Probation Office Information")
@JsonInclude(NON_NULL)
data class ProbationOfficeDto(
  @Schema(description = "Probation Office ID", example = "SHEFPB") val probationOfficeId: String,
  @Schema(description = "Name", example = "Sheffield Probation Office") val probationOfficeName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Probation Office") val description: String?,
  @Schema(description = "Contact", example = "John Smith") val contact: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Accessible access", example = "ACCESSIBLE") val accessibleAccess: String?,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Subarea") val subarea: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographical Area") val geographicalArea: CodeDescription?,
  @Schema(description = "Local Authority") val localAuthority: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses") val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)

@Schema(description = "Probation Office Update Record")
@JsonInclude(NON_NULL)
data class UpdateProbationOfficeDto(
  @Schema(description = "Name", example = "Sheffield Probation Office", required = true)
  @field:NotBlank(message = "Probation Office name is required")
  @field:Size(max = 40, message = "Probation Office name must be no more than 40 characters")
  val probationOfficeName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Probation Office")
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
  @Schema(description = "Subarea code", example = "SHEFF")
  @field:Size(max = 12, message = "Subarea code must be no more than 12 characters")
  val subareaCode: String?,
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

@Schema(description = "Probation Office Create Record")
@JsonInclude(NON_NULL)
data class CreateProbationOfficeDto(
  @Schema(description = "Probation Office ID", example = "SHEFPB", required = true)
  @field:NotBlank(message = "Probation Office id is required")
  @field:Size(min = 2, max = 6, message = "Probation Office Id must be between 2 and 6 characters")
  val probationOfficeId: String,
  @Schema(description = "Name", example = "Sheffield Probation Office", required = true)
  @field:NotBlank(message = "Probation Office name is required")
  @field:Size(max = 40, message = "Probation Office name must be no more than 40 characters")
  val probationOfficeName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Probation Office")
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
  @Schema(description = "Subarea code", example = "SHEFF")
  @field:Size(max = 12, message = "Subarea code must be no more than 12 characters")
  val subareaCode: String?,
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
