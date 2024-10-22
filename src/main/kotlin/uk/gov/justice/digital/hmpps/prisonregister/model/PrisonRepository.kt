package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PrisonRepository : JpaRepository<Prison, String>, JpaSpecificationExecutor<Prison> {

  @Query("FROM Prison p where (:active is null or p.active = :active) order by p.name")
  fun findByActiveOrderByPrisonId(active: Boolean?): List<Prison>

  fun findOneByGpPractice(gpPractice: String): Prison?

  fun findAllByPrisonIdIsIn(ids: List<String>): List<Prison>

  @Query("FROM Prison p left join fetch p.contactDetails where p.prisonId = :prisonId")
  fun findByPrisonIdWithContactDetails(prisonId: String): Prison? // Used in tests only
}
