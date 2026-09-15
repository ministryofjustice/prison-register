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
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_ADDRESS_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_ADDRESS_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_PHONE_DELETE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_PHONE_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.HOSPITAL_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.HospitalService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/hospitals", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class HospitalResource(
  private val hospitalService: HospitalService,
  private val auditService: AuditService,
  private val snsService: SnsService,
) {
  @GetMapping("/id/{hospitalId}")
  @Operation(summary = "Get specified hospital", description = "Information on a specific hospital")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getHospitalFromId(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
  ): HospitalDto = hospitalService.findById(hospitalId)

  @GetMapping
  @Operation(summary = "Get all hospitals", description = "Information on all hospitals")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getHospitals(): List<HospitalDto> = hospitalService.getAll()

  @Operation(
    summary = "Create a new hospital",
    description = "Creates a hospital, along with any addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateHospitalDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Hospital Created",
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
        description = "Incorrect permissions to create a hospital",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createHospital(
    @RequestBody @Valid
    createHospitalDto: CreateHospitalDto,
  ): HospitalDto {
    val createdHospital = hospitalService.createHospital(createHospitalDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterInsertedEvent(createHospitalDto.hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_INSERT.name,
      mapOf("hospitalId" to createHospitalDto.hospitalId, "hospital" to createHospitalDto),
      now,
    )
    return createdHospital
  }

  @Operation(
    summary = "Update specified hospital details",
    description = "Updates hospital information, excluding its addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateHospitalDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Hospital Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update hospital",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make hospital update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{hospitalId}")
  fun updateHospital(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @RequestBody @Valid
    updateHospitalDto: UpdateHospitalDto,
  ): HospitalDto {
    val updatedHospital = hospitalService.updateHospital(hospitalId, updateHospitalDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_UPDATE.name,
      mapOf("hospitalId" to hospitalId, "hospital" to updateHospitalDto),
      now,
    )
    return updatedHospital
  }

  @Operation(
    summary = "Create a hospital address",
    description = "Creates a new address for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Hospital Address Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create hospital address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a hospital address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{hospitalId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHospitalAddress(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val createdAddress = hospitalService.createHospitalAddress(hospitalId, updateAddressDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_ADDRESS_INSERT.name,
      mapOf("hospitalId" to hospitalId, "address" to createdAddress),
      now,
    )
    return createdAddress
  }

  @Operation(
    summary = "Update specified hospital address",
    description = "Updates a single address for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Hospital Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update hospital address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make hospital address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{hospitalId}/address/{addressId}")
  fun updateHospitalAddress(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = hospitalService.updateHospitalAddress(hospitalId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_ADDRESS_UPDATE.name,
      mapOf("hospitalId" to hospitalId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Create a hospital phone number",
    description = "Creates a new phone number for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Hospital Phone Number Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to create hospital phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create a hospital phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Phone number already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/id/{hospitalId}/phone-number")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHospitalPhoneNumber(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val createdPhoneNumber = hospitalService.createHospitalPhoneNumber(hospitalId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_PHONE_INSERT.name,
      mapOf("hospitalId" to hospitalId, "phoneNumber" to createdPhoneNumber),
      now,
    )
    return createdPhoneNumber
  }

  @Operation(
    summary = "Update specified hospital phone number",
    description = "Updates a single phone number for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Hospital Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update hospital phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make hospital phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{hospitalId}/phone-number/{phoneNumberId}")
  fun updateHospitalPhoneNumber(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = hospitalService.updateHospitalPhoneNumber(hospitalId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_PHONE_UPDATE.name,
      mapOf("hospitalId" to hospitalId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Delete specified hospital",
    description = "Deletes a hospital, along with any addresses and phone numbers associated with it. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Hospital Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a hospital",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{hospitalId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteHospital(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
  ) {
    hospitalService.deleteHospital(hospitalId)
    val now = Instant.now()
    snsService.sendHospitalRegisterDeletedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_DELETE.name,
      mapOf("hospitalId" to hospitalId),
      now,
    )
  }

  @Operation(
    summary = "Delete specified hospital address",
    description = "Deletes a single address for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Hospital Address Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a hospital address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{hospitalId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteHospitalAddress(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
  ) {
    val deletedAddress = hospitalService.deleteHospitalAddress(hospitalId, addressId)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_ADDRESS_DELETE.name,
      mapOf("hospitalId" to hospitalId, "address" to deletedAddress),
      now,
    )
  }

  @Operation(
    summary = "Delete specified hospital phone number",
    description = "Deletes a single phone number for a hospital. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Hospital Phone Number Deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to delete a hospital phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Hospital Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/id/{hospitalId}/phone-number/{phoneNumberId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteHospitalPhoneNumber(
    @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
    hospitalId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
  ) {
    val deletedPhoneNumber = hospitalService.deleteHospitalPhoneNumber(hospitalId, phoneNumberId)
    val now = Instant.now()
    snsService.sendHospitalRegisterAmendedEvent(hospitalId, now)
    auditService.sendAuditEvent(
      HOSPITAL_REGISTER_PHONE_DELETE.name,
      mapOf("hospitalId" to hospitalId, "phoneNumber" to deletedPhoneNumber),
      now,
    )
  }
}

@Schema(description = "Hospital Information")
@JsonInclude(NON_NULL)
data class HospitalDto(
  @Schema(description = "Hospital ID", example = "NWCLYC") val hospitalId: String,
  @Schema(description = "Name", example = "N Staffs Youth Hospital - Newcastle") val hospitalName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Hospital - Newcastle under Lyme") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31") val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789") val cjitCode: String?,
  @Schema(description = "Area") val area: CodeDescription?,
  @Schema(description = "Region") val region: CodeDescription?,
  @Schema(description = "Geographic Region") val geographicalArea: CodeDescription?,
  @Schema(description = "Local Authority") val localAuthority: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region") val payrollRegion: CodeDescription?,
  @Schema(description = "Is high security restricted hospital") val highSecurity: Boolean,
  @Schema(description = "addresses") val addresses: List<AgencyAddressDto>,
  @Schema(description = "phoneNumbers") val phoneNumbers: List<AgencyPhoneDto>,
)

@Schema(description = "Hospital Update Record")
@JsonInclude(NON_NULL)
data class UpdateHospitalDto(
  @Schema(description = "Name", example = "N Staffs Youth Hospital - Newcastle", required = true)
  @field:NotBlank(message = "Hospital name is required")
  @field:Size(max = 40, message = "Hospital name must be no more than 40 characters")
  val hospitalName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Hospital - Newcastle under Lyme")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean,
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
  @Schema(description = "Is high security restricted hospital", required = true)
  val highSecurity: Boolean,
)

@Schema(description = "Hospital Create Record")
@JsonInclude(NON_NULL)
data class CreateHospitalDto(
  @Schema(description = "Hospital ID", example = "SHEFCC", required = true)
  @field:NotBlank(message = "Hospital id is required")
  @field:Size(min = 2, max = 6, message = "Hospital Id must be between 2 and 6 letters")
  val hospitalId: String,
  @Schema(description = "Name", example = "N Staffs Youth Hospital - Newcastle", required = true)
  @field:NotBlank(message = "Hospital name is required")
  @field:Size(max = 40, message = "Hospital name must be no more than 40 characters")
  val hospitalName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Hospital - Newcastle under Lyme")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean = true,
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
  @Schema(description = "Is high security restricted hospital", required = true)
  val highSecurity: Boolean = false,
  @Schema(description = "Addresses")
  @field:Valid
  val addresses: List<UpdateAddressDto> = listOf(),
  @Schema(description = "Phone numbers")
  @field:Valid
  val phoneNumbers: List<UpdatePhoneNumberDto> = listOf(),
)
