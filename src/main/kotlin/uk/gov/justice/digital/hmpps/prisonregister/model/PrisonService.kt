package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import javax.persistence.EntityNotFoundException

const val CLIENT_CAN_MAINTAIN_EMAIL_ADDRESSES = "hasRole('MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')"

@Service
@Transactional(readOnly = true)
class PrisonService(
  private val prisonRepository: PrisonRepository,
  private val videoLinkConferencingCentreRepository: VideoLinkConferencingCentreRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository
) {
  fun findById(prisonId: String): Prison =
    prisonRepository.findByIdOrNull(prisonId) ?: throw EntityNotFoundException("Prison $prisonId not found")

  fun findByGpPractice(gpPracticeCode: String): Prison =
    prisonRepository.findByGpPracticeGpPracticeCode(gpPracticeCode)
      ?: throw EntityNotFoundException("Prison with gp practice $gpPracticeCode not found")

  fun findAll(): List<PrisonDto> = prisonRepository.findAll().map { PrisonDto(it) }

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
