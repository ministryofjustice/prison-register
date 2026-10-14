package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AreaRepository : JpaRepository<Area, String> {
  fun findAllByRegionCode(region: String, sort: Sort): List<Area>
}
