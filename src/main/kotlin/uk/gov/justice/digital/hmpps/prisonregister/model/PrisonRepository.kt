package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PrisonRepository : CrudRepository<Prison, String> {
  fun findByActiveOrderByPrisonId(active: Boolean): List<Prison>

  fun findByGpPracticeGpPracticeCode(gpPracticeCode: String): Prison?

  @Query(
    """
    select p from Prison p where p in (
      select distinct p from Prison p
      where (:active is null or p.active = :active)
      and (:textSearchUpperCase is null or (UPPER(p.prisonId) like %:textSearchUpperCase% or UPPER(p.name) like %:textSearchUpperCase%))
    ) order by p.prisonId
  """
  )
  fun findByActiveAndTextSearchOrderByPrisonId(
    @Param("active") active: Boolean?,
    @Param("textSearchUpperCase") textSearchUpperCase: String?
  ): List<Prison>
}
