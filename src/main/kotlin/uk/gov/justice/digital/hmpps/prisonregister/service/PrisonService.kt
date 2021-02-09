package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonregister.jpa.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import javax.persistence.EntityNotFoundException

@Service
class PrisonService(private val prisonRepository: PrisonRepository) {
  fun findById(prisonId: String): PrisonDto {
    val prison = prisonRepository.findById(prisonId)
      .orElseThrow { EntityNotFoundException("Prison $prisonId not found") }
    return PrisonDto(prison)
  }

  fun findAll(): List<PrisonDto> {
    return prisonRepository.findAll().map { PrisonDto(it) }
  }
}
