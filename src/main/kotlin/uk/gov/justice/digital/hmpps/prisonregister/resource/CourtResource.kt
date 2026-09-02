package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.AccessibleAccess
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyAddressDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.resource.validator.ValidPhoneNumber
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.COURT_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.COURT_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.COURT_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.COURT_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.CourtService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/courts", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class CourtResource(
  private val courtService: CourtService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @GetMapping("/id/{courtId}")
  @Operation(summary = "Get specified court", description = "Information on a specific court")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getCourtFromId(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
  ): CourtDto = courtService.findById(courtId)

  @GetMapping
  @Operation(summary = "Get all courts", description = "Information on all courts")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getCourts(): List<CourtDto> = courtService.getAll()

  @Operation(
    summary = "Update specified court details",
    description = "Updates court information, excluding its addresses. Requires role ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateCourtDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update court",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}")
  fun updateCourt(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
    @RequestBody @Valid
    updateCourtDto: UpdateCourtDto,
  ): CourtDto {
    val updatedCourt = courtService.updateCourt(courtId, updateCourtDto)
    val now = Instant.now()
    snsService.sendCourtRegisterAmendedEvent(courtId, now)
    auditService.sendAuditEvent(
      COURT_REGISTER_UPDATE.name,
      mapOf("courtId" to courtId, "court" to updateCourtDto),
      now,
    )
    return updatedCourt
  }

  @Operation(
    summary = "Update specified court address",
    description = "Updates a single address for a court. Requires role ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Court Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update court address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}/address/{addressId}")
  fun updateCourtAddress(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = courtService.updateCourtAddress(courtId, addressId, updateAddressDto)
    val now = Instant.now()
    snsService.sendCourtRegisterAmendedEvent(courtId, now)
    auditService.sendAuditEvent(
      COURT_REGISTER_ADDRESS_UPDATE.name,
      mapOf("courtId" to courtId, "address" to updatedAddress),
      now,
    )
    return updatedAddress
  }

  @Operation(
    summary = "Update specified court phone number",
    description = "Updates a single phone number for a court. Requires role ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Court Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update court phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}/phone-number/{phoneNumberId}")
  fun updateCourtPhoneNumber(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = courtService.updateCourtPhoneNumber(courtId, phoneNumberId, updatePhoneNumberDto)
    val now = Instant.now()
    snsService.sendCourtRegisterAmendedEvent(courtId, now)
    auditService.sendAuditEvent(
      COURT_REGISTER_PHONE_UPDATE.name,
      mapOf("courtId" to courtId, "phoneNumber" to updatedPhoneNumber),
      now,
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Update specified court email address",
    description = "Updates a single email address for a court. Requires role ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Court Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update court email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{courtId}/email-address/{emailAddressId}")
  fun updateCourtEmailAddress(
    @Schema(description = "Court ID", example = "SHEFCC", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Court Id must be between 2 and 6 letters")
    courtId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = courtService.updateCourtEmailAddress(courtId, emailAddressId, updateEmailAddressDto)
    val now = Instant.now()
    snsService.sendCourtRegisterAmendedEvent(courtId, now)
    auditService.sendAuditEvent(
      COURT_REGISTER_EMAIL_UPDATE.name,
      mapOf("courtId" to courtId, "emailAddress" to updatedEmailAddress),
      now,
    )
    return updatedEmailAddress
  }
}

@Schema(description = "Court Information")
@JsonInclude(NON_NULL)
data class CourtDto(
  @Schema(description = "Court ID", example = "NWCLYC")
  val courtId: String,
  @Schema(description = "Name", example = "N Staffs Youth Court - Newcastle")
  val courtName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Court - Newcastle under Lyme")
  val description: String?,
  @Schema(description = "Whether still active")
  val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31")
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789")
  val cjitCode: String?,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: String?,
  @Schema(description = "Area")
  val area: CodeDescription?,
  @Schema(description = "Region")
  val region: CodeDescription?,
  @Schema(description = "Geographical Area")
  val geographicalArea: CodeDescription?,
  @Schema(description = "Local Authority")
  val localAuthority: CodeDescription?,
  @Schema(description = "Prisoner Payroll Region")
  val payrollRegion: CodeDescription?,
  @Schema(description = "courtType")
  val courtType: CodeDescription?,
  @Schema(description = "addresses")
  val addresses: List<AgencyAddressDto>,
  @Schema(description = "emailAddresses")
  val emailAddresses: List<AgencyEmailDto>,
  @Schema(description = "phoneNumbers")
  val phoneNumbers: List<AgencyPhoneDto>,
)

@Schema(description = "Court Update Record")
@JsonInclude(NON_NULL)
data class UpdateCourtDto(
  @Schema(description = "Name", example = "N Staffs Youth Court - Newcastle", required = true)
  @field:NotBlank(message = "Court name is required")
  @field:Size(max = 40, message = "Court name must be no more than 40 characters")
  val courtName: String,
  @Schema(description = "Description", example = "North Staffordshire Youth Court - Newcastle under Lyme")
  @field:Size(max = 3000, message = "Description must be no more than 3000 characters")
  val description: String?,
  @Schema(description = "Whether still active", required = true)
  val active: Boolean,
  @Schema(description = "Date made inactive", example = "2023-12-31")
  val inactiveDate: LocalDate?,
  @Schema(description = "CJIT Code", example = "123456789")
  @field:Size(max = 12, message = "CJIT code must be no more than 12 characters")
  val cjitCode: String?,
  @Schema(description = "Accessible access", example = "ACCESSIBLE")
  val accessibleAccess: AccessibleAccess?,
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
  @Schema(description = "Court Type code", example = "CC", required = true)
  @field:NotBlank(message = "Court type code is required")
  @field:Size(max = 12, message = "Court type code must be no more than 12 characters")
  val courtTypeCode: String,
)

@Schema(description = "Phone Number Update Record")
@JsonInclude(NON_NULL)
data class UpdatePhoneNumberDto(
  @Schema(description = "Phone number", example = "0114 555 9898", required = true)
  @field:NotBlank(message = "Phone number is required")
  @field:Size(max = 100, message = "Phone number must be no more than 100 characters")
  @field:ValidPhoneNumber
  val number: String,
)

@Schema(description = "Email Address Update Record")
@JsonInclude(NON_NULL)
data class UpdateEmailAddressDto(
  @Schema(description = "Email address", example = "example@example.com", required = true)
  @field:NotBlank(message = "Email address is required")
  @field:Size(max = 100, message = "Email address must be no more than 100 characters")
  @field:Email(message = "Email address is in an incorrect format")
  val address: String,
)
