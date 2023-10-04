package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContactDetailsRepository : JpaRepository<ContactDetails, Long> {

  @Query(
    "SELECT c.emailAddress.value FROM ContactDetails c WHERE c.prisonId=:prisonId AND c.type=:contactPurposeType",
  )
  fun getEmailAddressByPrisonerIdAndPurpose(prisonId: String, contactPurposeType: ContactPurposeType): String?

  @Query(
    "SELECT count(c) = 0 FROM ContactDetails c WHERE c.emailAddress.value=:emailAddress",
  )
  fun isEmailOrphaned(emailAddress: String): Boolean

  fun getByPrisonIdAndType(prisonId: String, contactPurposeType: ContactPurposeType): ContactDetails?
}