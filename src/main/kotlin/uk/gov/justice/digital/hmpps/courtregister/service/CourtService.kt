package uk.gov.justice.digital.hmpps.courtregister.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.courtregister.jpa.CourtRepository
import uk.gov.justice.digital.hmpps.courtregister.resource.CourtDto
import javax.persistence.EntityNotFoundException

@Service
class CourtService(private val courtRepository: CourtRepository) {
  fun findById(courtId: String): CourtDto {
    val court = courtRepository.findById(courtId)
      .orElseThrow { EntityNotFoundException("Court $courtId not found") }
    return CourtDto(court)
  }

  fun findAll(): List<CourtDto> {
    return courtRepository.findAll().map { CourtDto(it) }
  }
}
