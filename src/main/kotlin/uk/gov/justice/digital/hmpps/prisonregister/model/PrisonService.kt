package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import java.util.Optional
import javax.persistence.EntityNotFoundException

const val HAS_MAINTAIN_REF_DATA_ROLE = "hasRole('MAINTAIN_REF_DATA')"
@Service
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository
) {
  @Transactional(readOnly = true)
  fun findById(prisonId: String): PrisonDto {
    val prison = prisonRepository.findById(prisonId)
      .orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return PrisonDto(prison)
  }

  @Transactional(readOnly = true)
  fun findAll(): List<PrisonDto> {
    return prisonRepository.findAll().map { PrisonDto(it) }
  }

  @Transactional(readOnly = true)
  fun getVccEmailAddress(prisonId: String): Optional<String> = videoLinkConferencingCentreRepository
    .findById(prisonId)
    .map { vcc -> vcc.emailAddress }

  @Transactional(readOnly = true)
  fun getOmuEmailAddress(prisonId: String): Optional<String> = offenderManagementUnitRepository
    .findById(prisonId)
    .map { omu -> omu.emailAddress }

  @Transactional
  @PreAuthorize(HAS_MAINTAIN_REF_DATA_ROLE)
  fun setVccEmailAddress(prisonId: String, emailAddress: String): SetOutcome {
    val vccOpt = videoLinkConferencingCentreRepository.findById(prisonId)
    if (vccOpt.isPresent) {
      vccOpt.get().emailAddress = emailAddress
      return SetOutcome.UPDATED
    } else {
      val prison = prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException() }
      videoLinkConferencingCentreRepository.save(VideolinkConferencingCentre(prison, emailAddress))
      return SetOutcome.CREATED
    }
  }

  @Transactional
  @PreAuthorize(HAS_MAINTAIN_REF_DATA_ROLE)
  fun setOmuEmailAddress(prisonId: String, emailAddress: String): SetOutcome {
    val omuOpt = offenderManagementUnitRepository.findById(prisonId)
    return if (omuOpt.isPresent) {
      omuOpt.get().emailAddress = emailAddress
      SetOutcome.UPDATED
    } else {
      val prison = prisonRepository.findById(prisonId).orElseThrow { EntityNotFoundException() }
      offenderManagementUnitRepository.save(OffenderManagementUnit(prison, emailAddress))
      SetOutcome.CREATED
    }
  }

  @Transactional
  @PreAuthorize(HAS_MAINTAIN_REF_DATA_ROLE)
  fun deleteOmuEmailAddress(prisonId: String) {
    offenderManagementUnitRepository
      .findById(prisonId)
      .ifPresent {
        offenderManagementUnitRepository.deleteById(prisonId)
      }
  }

  @Transactional
  @PreAuthorize(HAS_MAINTAIN_REF_DATA_ROLE)
  fun deleteVccEmailAddress(prisonId: String) {
    videoLinkConferencingCentreRepository
      .findById(prisonId)
      .ifPresent {
        videoLinkConferencingCentreRepository.deleteById(prisonId)
      }
  }
}
