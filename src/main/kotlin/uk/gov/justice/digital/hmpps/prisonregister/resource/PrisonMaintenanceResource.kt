package uk.gov.justice.digital.hmpps.prisonregister.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.ErrorResponse
import uk.gov.justice.digital.hmpps.prisonregister.model.Category
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditService
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PRISON_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.prisonregister.service.AuditType.PRISON_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.prisonregister.service.CLIENT_CAN_MAINTAIN_PRISON_DETAILS
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService
import uk.gov.justice.digital.hmpps.prisonregister.service.SnsService
import java.time.Instant

@RestController
@Validated
@RequestMapping(name = "Prison Maintenance", path = ["/prison-maintenance"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonMaintenanceResource(
  private val prisonService: PrisonService,
  private val snsService: SnsService,
  private val auditService: AuditService,
) {
  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Adds a new prison",
    description = "Adds new prison information, role required is MAINTAIN_REF_DATA or MAINTAIN_PRISON_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"]), SecurityRequirement(name = "MAINTAIN_PRISON_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = InsertPrisonDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Prison Information Inserted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to add prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make prison insert",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("")
  fun insertPrison(
    @RequestBody @Valid
    prisonInsertRecord: InsertPrisonDto,
  ): PrisonDto {
    val insertedPrison = prisonService.findById(prisonService.insertPrison(prisonInsertRecord))

    val now = Instant.now()

    snsService.sendPrisonRegisterInsertedEvent(insertedPrison.prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_INSERT.name,
      prisonInsertRecord,
      now,
    )
    return insertedPrison
  }

  @PreAuthorize(CLIENT_CAN_MAINTAIN_PRISON_DETAILS)
  @Operation(
    summary = "Update specified prison details",
    description = "Updates prison information, role required is MAINTAIN_REF_DATA or MAINTAIN_PRISON_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"]), SecurityRequirement(name = "MAINTAIN_PRISON_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdatePrisonDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prison Information Updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Information request to update prison",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make prison update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prison ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping("/id/{prisonId}")
  fun updatePrison(
    @Schema(description = "Prison Id", example = "MDI", required = true)
    @PathVariable
    @Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 letters")
    prisonId: String,
    @RequestBody @Valid
    prisonUpdateRecord: UpdatePrisonDto,
  ): PrisonDto {
    val updatedPrison = prisonService.updatePrison(prisonId, prisonUpdateRecord)
    val now = Instant.now()
    snsService.sendPrisonRegisterAmendedEvent(prisonId, now)
    auditService.sendAuditEvent(
      PRISON_REGISTER_UPDATE.name,
      prisonId to prisonUpdateRecord,
      now,
    )
    return updatedPrison
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Insert Record")
data class InsertPrisonDto(
  @Schema(description = "Prison Id", example = "MDI", required = true)
  @field:Size(min = 3, max = 6, message = "Prison Id must be between 3 and 6 letters")
  @field:NotBlank
  val prisonId: String,

  @Schema(description = "Name of the prison", example = "HMP Moorland", required = true)
  @field:Size(min = 3, max = 40, message = "Prison name must be between 3 and 40 letters")
  @field:NotBlank(message = "Prison name is required")
  val prisonName: String,

  @Schema(description = "Welsh name of the prison", example = "Carchar Brynbuga", required = false)
  @field:Size(min = 3, max = 40, message = "Welsh prison name must be between 3 and 40 letters")
  val prisonNameInWelsh: String? = null,

  @Schema(description = "Whether the prison is still active", required = false)
  val active: Boolean = true,

  @Schema(description = "If this is a male prison", required = false)
  val male: Boolean = false,

  @Schema(description = "If this is a female prison", required = false)
  val female: Boolean = false,

  @Schema(description = "If this is a contracted prison", required = true)
  @field:NotNull(message = "Prison contracted status is required")
  val contracted: Boolean,

  @Schema(description = "If this prison is part of the long term high security estate", required = false)
  val lthse: Boolean = false,

  @Schema(description = "Set of types for this prison", example = "HMP")
  val prisonTypes: Set<Type> = setOf(),

  @Schema(description = "List of addresses for this prison", required = false)
  val addresses: List<UpdateAddressDto> = listOf(),

  @Schema(description = "Set of categories for this prison", required = false)
  val categories: Set<Category> = setOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Update Record")
data class UpdatePrisonDto(
  @Schema(description = "Name of the prison", example = "HMP Moorland", required = true)
  @field:Size(min = 3, max = 40, message = "Prison name must be between 3 and 40 letters")
  @field:NotBlank(message = "Prison name is required")
  val prisonName: String,

  @Schema(description = "Welsh name of the prison", example = "Carchar Brynbuga", required = false)
  @field:Size(min = 3, max = 40, message = "Welsh prison name must be between 3 and 40 letters")
  val prisonNameInWelsh: String? = null,

  @Schema(description = "Whether the prison is still active", required = true)
  val active: Boolean,

  @Schema(description = "If this is a male prison", required = false)
  val male: Boolean = false,

  @Schema(description = "If this is a female prison", required = false)
  val female: Boolean = false,

  @Schema(description = "If this is a contracted prison", required = false)
  val contracted: Boolean = false,

  @Schema(description = "If this prison is part of the long term high security estate", required = false)
  val lthse: Boolean = false,

  @Schema(description = "Set of types for this prison")
  val prisonTypes: Set<Type> = setOf(),

  @Schema(description = "Set of categories for this prison", required = false)
  val categories: Set<Category> = setOf(),
)
