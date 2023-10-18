package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress

@Repository
interface TestWebAddressRepository : JpaRepository<EmailAddress, Long> {

  @Query(
    "SELECT count(wa) FROM WebAddress wa WHERE wa.value=:webAddress",
  )
  fun getWebAddressCount(webAddress: String): Int
}
