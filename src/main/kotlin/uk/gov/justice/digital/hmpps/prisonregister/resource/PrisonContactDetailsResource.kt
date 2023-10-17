package uk.gov.justice.digital.hmpps.prisonregister.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonregister.model.DepartmentType
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.ContactDetailsRequestDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.UpdateContactDetailsDto
import uk.gov.justice.digital.hmpps.prisonregister.service.PrisonService

private const val SECURE_PRISON_BY_ID = "secure/prisons/id/{prisonId}"
private const val END_POINT = "/$SECURE_PRISON_BY_ID/department/contact-details"

@RestController
@Validated
class PrisonContactDetailsResource(private val prisonService: PrisonService) {

  @GetMapping(
    END_POINT,
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Get a prison department's contact details",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ContactDetailsRequestDto::class),
        ),
      ],
    ),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the departments contact details",
        content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have contact details for this department",
      ),
    ],
  )
  fun getContactDetails(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "Department type", example = "SOCIAL_VISIT", defaultValue = "true", required = false)
    @RequestParam
    @NotNull
    departmentType: DepartmentType,
  ): ResponseEntity<ContactDetailsDto> {
    val contactDetails = prisonService.get(prisonId, departmentType)
    return ResponseEntity.ok(contactDetails)
  }

  @PostMapping(
    END_POINT,
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Create a prison department's contact details",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ContactDetailsDto::class),
        ),
      ],
    ),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Contact details have been created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, contact details, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun createContactDetails(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @RequestBody @Valid
    contactDetailsDto: ContactDetailsDto,
  ): ResponseEntity<ContactDetailsDto> {
    return ResponseEntity<ContactDetailsDto>(prisonService.createContactDetails(prisonId, contactDetailsDto), HttpStatus.CREATED)
  }

  @PutMapping(
    END_POINT,
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @Operation(
    summary = "Change a prison department's contact details",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateContactDetailsDto::class),
        ),
      ],
    ),
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "The phone number was created",
      ),
      ApiResponse(
        responseCode = "204",
        description = "The phone number was updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId, phone number, media type or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun updateContactDetails(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @RequestBody @Valid
    updateContactDetailsDto: UpdateContactDetailsDto,
    @Schema(description = "if true individual contact details are removed if null", example = "true", defaultValue = "true", required = false)
    @RequestParam
    removeIfNull: Boolean = true,
  ): ContactDetailsDto {
    return prisonService.updateContactDetails(prisonId, updateContactDetailsDto, removeIfNull)
  }

  @DeleteMapping(
    END_POINT,
  )
  @Operation(
    summary = "Remove a prison department's contact details",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The contact details were removed",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Client error - invalid prisonId or similar",
      ),
      ApiResponse(
        responseCode = "404",
        description = "The prison does not have a phone number for this department",
      ),
    ],
  )
  fun deletePhoneNumber(
    @Schema(description = "Prison ID", example = "MDI", required = true)
    @PathVariable
    @Size(max = 12, min = 2)
    prisonId: String,
    @Schema(description = "Department type", example = "SOCIAL_VISIT", defaultValue = "true", required = false)
    @RequestParam
    @NotNull
    departmentType: DepartmentType,
  ): ResponseEntity<HttpStatus> {
    prisonService.deleteContactDetails(prisonId, departmentType)
    return ResponseEntity(HttpStatus.NO_CONTENT)
  }
}