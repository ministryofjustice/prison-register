package uk.gov.justice.digital.hmpps.prisonregister.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.Gender
import uk.gov.justice.digital.hmpps.prisonregister.model.OffenderManagementUnit
import uk.gov.justice.digital.hmpps.prisonregister.model.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonType
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonFilter
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.SetOutcome
import uk.gov.justice.digital.hmpps.prisonregister.model.Type
import uk.gov.justice.digital.hmpps.prisonregister.model.VideoLinkConferencingCentreRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.VideolinkConferencingCentre
import uk.gov.justice.digital.hmpps.prisonregister.resource.GpDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.InsertPrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdatePrisonDto
import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

const val CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES = "hasRole('MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')"

@Service
@Transactional(readOnly = true)
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  private val telemetryClient: TelemetryClient
) {
  fun findById(prisonId: String): PrisonDto {
    val prison = prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return PrisonDto(prison)
  }

  fun findPrisonAndGpPracticeById(prisonId: String): GpDto {
    val prison = prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return GpDto(prison)
  }

  fun findByGpPractice(gpPracticeCode: String): GpDto {
    val prison = prisonRepository.findByGpPracticeGpPracticeCode(gpPracticeCode)
      ?: throw EntityNotFoundException("Prison with gp practice $gpPracticeCode not found")
    return GpDto(prison)
  }

  fun findAll(): List<PrisonDto> = prisonRepository.findAll().map { PrisonDto(it) }

  fun findByPrisonFilter(
    active: Boolean? = null,
    textSearch: String? = null,
    genders: List<Gender>? = listOf(),
    prisonTypeCodes: List<Type>? = listOf(),
  ): List<PrisonDto> =
    prisonRepository.findAll(PrisonFilter(active, textSearch, genders, prisonTypeCodes)).map { PrisonDto(it) }

  @Transactional
  fun insertPrison(prisonInsertRecord: InsertPrisonDto): String {
    if (prisonRepository.findById(prisonInsertRecord.prisonId).isPresent) {
      throw EntityExistsException("Prison $prisonInsertRecord.prisonId already exists")
    }

    with(prisonInsertRecord) {
      val prison = Prison(prisonId = prisonId, name = prisonName, active = active)
      telemetryClient.trackEvent("prison-register-insert", mapOf("prison" to prison.name), null)
      return prisonRepository.save(prison).prisonId
    }
  }

  @Transactional
  fun updatePrison(prisonId: String, prisonUpdateRecord: UpdatePrisonDto): PrisonDto {
    val prison = prisonRepository.findById(prisonId)
      .orElseThrow { EntityNotFoundException("Prison $prisonId not found") }

    with(prisonUpdateRecord) {
      prison.name = prisonName
      prison.active = active

      val updatedTypes = prisonTypes.map { PrisonType(type = it, prison = prison) }.toSet()
      prison.prisonTypes.retainAll(updatedTypes)
      prison.prisonTypes.addAll(updatedTypes)
    }
    telemetryClient.trackEvent("prison-register-update", mapOf("prison" to prison.name), null)
    return PrisonDto(prison)
  }

  fun getVccEmailAddress(prisonId: String): String? = videoLinkConferencingCentreRepository
    .findByIdOrNull(prisonId)
    ?.run { emailAddress }

  fun getOmuEmailAddress(prisonId: String): String? = offenderManagementUnitRepository
    .findByIdOrNull(prisonId)
    ?.run { emailAddress }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES)
  fun setVccEmailAddress(prisonId: String, emailAddress: String): SetOutcome {
    val vcc = videoLinkConferencingCentreRepository.findByIdOrNull(prisonId)
    return if (vcc == null) {
      val prison = prisonRepository.findByIdOrNull(prisonId) ?: throw EntityNotFoundException()
      videoLinkConferencingCentreRepository.save(VideolinkConferencingCentre(prison, emailAddress))
      SetOutcome.CREATED
    } else {
      vcc.emailAddress = emailAddress
      SetOutcome.UPDATED
    }
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES)
  fun setOmuEmailAddress(prisonId: String, emailAddress: String): SetOutcome {
    val omu = offenderManagementUnitRepository.findByIdOrNull(prisonId)
    return if (omu != null) {
      omu.emailAddress = emailAddress
      SetOutcome.UPDATED
    } else {
      val prison = prisonRepository.findByIdOrNull(prisonId) ?: throw EntityNotFoundException()
      offenderManagementUnitRepository.save(OffenderManagementUnit(prison, emailAddress))
      SetOutcome.CREATED
    }
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES)
  fun deleteOmuEmailAddress(prisonId: String) {
    if (offenderManagementUnitRepository.existsById(prisonId)) {
      offenderManagementUnitRepository.deleteById(prisonId)
    }
  }

  @Transactional
  @PreAuthorize(CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES)
  fun deleteVccEmailAddress(prisonId: String) {
    if (videoLinkConferencingCentreRepository.existsById(prisonId)) {
      videoLinkConferencingCentreRepository.deleteById(prisonId)
    }
  }
}
