package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocalAuthorityRepository : JpaRepository<LocalAuthority, String> {
  fun findAllByActiveTrue(sort: Sort): List<LocalAuthority>
}
