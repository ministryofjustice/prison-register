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
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyEmailDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.AgencyPhoneDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.POLICE_CUSTODY_SUITE_REGISTER_ADDRESS_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.POLICE_CUSTODY_SUITE_REGISTER_EMAIL_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.POLICE_CUSTODY_SUITE_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.POLICE_CUSTODY_SUITE_REGISTER_PHONE_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.POLICE_CUSTODY_SUITE_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.PoliceCustodySuiteService
import java.time.Instant
import java.time.LocalDate

@RestController
@Validated
@RequestMapping("/police-custody-suites", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_HMPPS_REGISTERS_API__SYNCHRONISATION__RW')")
class PoliceCustodySuiteResource(
  private val policeCustodySuiteService: PoliceCustodySuiteService,
  private val auditService: AuditService,
) {
  @GetMapping("/id/{policeCustodySuiteId}")
  @Operation(summary = "Get specified police custody suite", description = "Information on a specific police custody suite")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getPoliceCustodySuiteFromId(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
  ): PoliceCustodySuiteDto = policeCustodySuiteService.findById(policeCustodySuiteId)

  @GetMapping
  @Operation(summary = "Get all police custody suites", description = "Information on all police custody suites")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful Operation",
      ),
    ],
  )
  fun getPoliceCustodySuites(): List<PoliceCustodySuiteDto> = policeCustodySuiteService.getAll()

  @Operation(
    summary = "Create a new police custody suite",
    description = "Creates a police custody suite, along with any addresses, email addresses and phone numbers supplied. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreatePoliceCustodySuiteDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Police Custody Suite Created",
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
        description = "Incorrect permissions to create a police custody suite",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createPoliceCustodySuite(
    @RequestBody @Valid
    createPoliceCustodySuiteDto: CreatePoliceCustodySuiteDto,
  ): PoliceCustodySuiteDto {
    val createdPoliceCustodySuite = policeCustodySuiteService.createPoliceCustodySuite(createPoliceCustodySuiteDto)
    auditService.sendAuditEvent(
      POLICE_CUSTODY_SUITE_REGISTER_INSERT.name,
      mapOf("policeCustodySuiteId" to createPoliceCustodySuiteDto.policeCustodySuiteId, "policeCustodySuite" to createPoliceCustodySuiteDto),
      Instant.now(),
    )
    return createdPoliceCustodySuite
  }

  @Operation(
    summary = "Update specified police custody suite details",
    description = "Updates police custody suite information, excluding its addresses, email addresses and phone numbers. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdatePoliceCustodySuiteDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Police Custody Suite Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update police custody suite",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make police custody suite update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Police Custody Suite Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{policeCustodySuiteId}")
  fun updatePoliceCustodySuite(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
    @RequestBody @Valid
    updatePoliceCustodySuiteDto: UpdatePoliceCustodySuiteDto,
  ): PoliceCustodySuiteDto {
    val updatedPoliceCustodySuite = policeCustodySuiteService.updatePoliceCustodySuite(policeCustodySuiteId, updatePoliceCustodySuiteDto)
    auditService.sendAuditEvent(
      POLICE_CUSTODY_SUITE_REGISTER_UPDATE.name,
      mapOf("policeCustodySuiteId" to policeCustodySuiteId, "policeCustodySuite" to updatePoliceCustodySuiteDto),
      Instant.now(),
    )
    return updatedPoliceCustodySuite
  }

  @Operation(
    summary = "Update specified police custody suite address",
    description = "Updates a single address for a police custody suite. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Police Custody Suite Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update police custody suite address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make police custody suite address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Police Custody Suite Id or Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{policeCustodySuiteId}/address/{addressId}")
  fun updatePoliceCustodySuiteAddress(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
    @Schema(description = "Address Id", example = "234231", required = true)
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    updateAddressDto: UpdateAddressDto,
  ): AgencyAddressDto {
    val updatedAddress = policeCustodySuiteService.updatePoliceCustodySuiteAddress(policeCustodySuiteId, addressId, updateAddressDto)
    auditService.sendAuditEvent(
      POLICE_CUSTODY_SUITE_REGISTER_ADDRESS_UPDATE.name,
      mapOf("policeCustodySuiteId" to policeCustodySuiteId, "address" to updatedAddress),
      Instant.now(),
    )
    return updatedAddress
  }

  @Operation(
    summary = "Update specified police custody suite phone number",
    description = "Updates a single phone number for a police custody suite. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Police Custody Suite Phone Number Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update police custody suite phone number",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make police custody suite phone number update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Police Custody Suite Id or Phone Number Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{policeCustodySuiteId}/phone-number/{phoneNumberId}")
  fun updatePoliceCustodySuitePhoneNumber(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
    @Schema(description = "Phone Number Id", example = "234231", required = true)
    @PathVariable
    phoneNumberId: Long,
    @RequestBody @Valid
    updatePhoneNumberDto: UpdatePhoneNumberDto,
  ): AgencyPhoneDto {
    val updatedPhoneNumber = policeCustodySuiteService.updatePoliceCustodySuitePhoneNumber(policeCustodySuiteId, phoneNumberId, updatePhoneNumberDto)
    auditService.sendAuditEvent(
      POLICE_CUSTODY_SUITE_REGISTER_PHONE_UPDATE.name,
      mapOf("policeCustodySuiteId" to policeCustodySuiteId, "phoneNumber" to updatedPhoneNumber),
      Instant.now(),
    )
    return updatedPhoneNumber
  }

  @Operation(
    summary = "Update specified police custody suite email address",
    description = "Updates a single email address for a police custody suite. Requires role HMPPS_REGISTERS_API__SYNCHRONISATION__RW",
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
        description = "Police Custody Suite Email Address Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad information provided to update police custody suite email address",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make police custody suite email address update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Police Custody Suite Id or Email Address Id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{policeCustodySuiteId}/email-address/{emailAddressId}")
  fun updatePoliceCustodySuiteEmailAddress(
    @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
    @PathVariable
    @Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
    policeCustodySuiteId: String,
    @Schema(description = "Email Address Id", example = "234231", required = true)
    @PathVariable
    emailAddressId: Long,
    @RequestBody @Valid
    updateEmailAddressDto: UpdateEmailAddressDto,
  ): AgencyEmailDto {
    val updatedEmailAddress = policeCustodySuiteService.updatePoliceCustodySuiteEmailAddress(policeCustodySuiteId, emailAddressId, updateEmailAddressDto)
    auditService.sendAuditEvent(
      POLICE_CUSTODY_SUITE_REGISTER_EMAIL_UPDATE.name,
      mapOf("policeCustodySuiteId" to policeCustodySuiteId, "emailAddress" to updatedEmailAddress),
      Instant.now(),
    )
    return updatedEmailAddress
  }
}

@Schema(description = "Police Custody Suite Information")
@JsonInclude(NON_NULL)
data class PoliceCustodySuiteDto(
  @Schema(description = "Police Custody Suite ID", example = "SHFPCS") val policeCustodySuiteId: String,
  @Schema(description = "Name", example = "Sheffield Police Custody Suite") val policeCustodySuiteName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Police Custody Suite") val description: String?,
  @Schema(description = "Whether still active") val active: Boolean,
  val inactiveDate: LocalDate?,
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

@Schema(description = "Police Custody Suite Update Record")
@JsonInclude(NON_NULL)
data class UpdatePoliceCustodySuiteDto(
  @Schema(description = "Name", example = "Sheffield Police Custody Suite", required = true)
  @field:NotBlank(message = "Police Custody Suite name is required")
  @field:Size(max = 40, message = "Police Custody Suite name must be no more than 40 characters")
  val policeCustodySuiteName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Police Custody Suite")
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
)

@Schema(description = "Police Custody Suite Create Record")
@JsonInclude(NON_NULL)
data class CreatePoliceCustodySuiteDto(
  @Schema(description = "Police Custody Suite ID", example = "SHFPCS", required = true)
  @field:NotBlank(message = "Police Custody Suite id is required")
  @field:Size(min = 2, max = 6, message = "Police Custody Suite Id must be between 2 and 6 letters")
  val policeCustodySuiteId: String,
  @Schema(description = "Name", example = "Sheffield Police Custody Suite", required = true)
  @field:NotBlank(message = "Police Custody Suite name is required")
  @field:Size(max = 40, message = "Police Custody Suite name must be no more than 40 characters")
  val policeCustodySuiteName: String,
  @Schema(description = "Description", example = "Sheffield City Centre Police Custody Suite")
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
