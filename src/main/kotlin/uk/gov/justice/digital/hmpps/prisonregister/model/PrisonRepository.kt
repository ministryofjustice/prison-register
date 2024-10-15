package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto

@Repository
interface PrisonRepository : JpaRepository<Prison, String>, JpaSpecificationExecutor<Prison> {
  fun findByActiveOrderByPrisonId(active: Boolean): List<Prison>

  fun findByPrisonId(prisonCode: String): Prison?

  fun findByGpPracticeGpPracticeCode(gpPracticeCode: String): Prison?

  fun findAllByPrisonIdIsIn(ids: List<String>): List<Prison>

  @Query(
    "SELECT new uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto(p.prisonId, p.name) FROM Prison p WHERE (:active IS NULL OR p.active = :active) ORDER BY p.name",
  )
  fun getPrisonNames(@Param("active") active: Boolean?): List<PrisonNameDto>

}