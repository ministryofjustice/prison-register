package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonregister.model.TelephoneAddress

@Repository
interface TestTelephoneAddressRepository : JpaRepository<TelephoneAddress, Long> {

  @Query(
    "SELECT count(ta) FROM TelephoneAddress ta WHERE ta.value=:emailAddress",
  )
  fun getTelephoneAddressCount(emailAddress: String): Int
}
