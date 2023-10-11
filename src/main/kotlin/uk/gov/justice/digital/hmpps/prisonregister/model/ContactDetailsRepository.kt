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
    "SELECT c.phoneNumber.value FROM ContactDetails c WHERE c.prisonId=:prisonId AND c.type=:departmentType",
  )
  fun getPhoneNumberByPrisonIdAndDepartment(prisonId: String, departmentType: DepartmentType): String?

  @Query(
    "SELECT count(c) = 0 FROM ContactDetails c WHERE c.phoneNumber.value=:phoneNumber",
  )
  fun isPhoneNumberOrphaned(phoneNumber: String): Boolean

  fun getByPrisonIdAndType(prisonId: String, departmentType: DepartmentType): ContactDetails?
}
