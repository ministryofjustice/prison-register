package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ContactDetailsRepository : JpaRepository<ContactDetails, Long> {

  @Query(
    "SELECT c.emailAddress.value FROM ContactDetails c WHERE c.prisonId=:prisonId AND c.type=:departmentType",
  )
  fun getEmailAddressByPrisonIdAndDepartment(prisonId: String, departmentType: DepartmentType): String?

  @Query(
    "SELECT count(c) = 0 FROM ContactDetails c WHERE c.emailAddress.value=:emailAddress",
  )
  fun isEmailOrphaned(emailAddress: String): Boolean

  @Query(
    "SELECT c FROM ContactDetails c WHERE c.prisonId=:prisonId AND c.type=:departmentType",
  )
  fun get(prisonId: String, departmentType: DepartmentType): ContactDetails?

  @Query(
    "SELECT count(c) = 0 FROM ContactDetails c WHERE c.phoneNumber.value=:phoneNumber",
  )
  fun isPhoneNumberOrphaned(phoneNumber: String): Boolean

  @Query(
    "SELECT count(c) = 0 FROM ContactDetails c WHERE c.webAddress.value=:webAddress",
  )
  fun isWebAddressOrphaned(webAddress: String): Boolean

  fun getByPrisonIdAndType(prisonId: String, departmentType: DepartmentType): ContactDetails?
}
