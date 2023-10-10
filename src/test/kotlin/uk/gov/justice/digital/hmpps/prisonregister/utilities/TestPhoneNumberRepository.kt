package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonregister.model.PhoneNumber

@Repository
interface TestPhoneNumberRepository : JpaRepository<PhoneNumber, Long> {

  @Query(
    "SELECT count(pn) FROM PhoneNumber pn WHERE pn.value=:phoneNumber",
  )
  fun getPhoneNumberCount(phoneNumber: String): Int
}
