package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonregister.model.EmailAddress

@Repository
interface TestEmailAddressRepository : JpaRepository<EmailAddress, Long> {

  @Query(
    "SELECT count(ea) FROM EmailAddress ea WHERE ea.value=:emailAddress",
  )
  fun getEmailCount(emailAddress: String): Int
}
