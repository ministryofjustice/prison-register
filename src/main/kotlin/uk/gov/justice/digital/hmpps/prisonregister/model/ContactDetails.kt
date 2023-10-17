package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreRemove
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "CONTACT_DETAILS", uniqueConstraints = [UniqueConstraint(columnNames = ["prison_id", "department_type"])])
class ContactDetails(
  @Column(name = "PRISON_ID", nullable = false)
  val prisonId: String,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
  @JoinColumn(name = "PRISON_ID", updatable = false, insertable = false)
  val prison: Prison,

  @Enumerated(EnumType.STRING)
  @Column(name = "department_type", columnDefinition = "enum('SOCIAL_VISIT','VIDEOLINK_CONFERENCING_CENTRE','OFFENDER_MANAGEMENT_UNIT')", nullable = false)
  var type: DepartmentType,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.DETACH], optional = true)
  @JoinTable(
    name = "CONTACT_DETAILS_TO_EMAIL_ADDRESS",
    joinColumns = [JoinColumn(name = "contact_details_id")],
    inverseJoinColumns = [JoinColumn(name = "email_address_id")],
  )
  var emailAddress: EmailAddress? = null,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.DETACH], optional = true)
  @JoinTable(
    name = "CONTACT_DETAILS_TO_PHONE_NUMBER",
    joinColumns = [JoinColumn(name = "contact_details_id")],
    inverseJoinColumns = [JoinColumn(name = "phone_number_id")],
  )
  var phoneNumber: PhoneNumber? = null,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.DETACH], optional = true)
  @JoinTable(
    name = "CONTACT_DETAILS_TO_WEB_ADDRESS",
    joinColumns = [JoinColumn(name = "contact_details_id")],
    inverseJoinColumns = [JoinColumn(name = "web_address_id")],
  )
  var webAddress: WebAddress? = null,

) : AbstractIdEntity() {

  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  @UpdateTimestamp
  @Column
  val modifyTimestamp: LocalDateTime? = null

  @PreRemove
  private fun removeFromParent() {
    prison.contactDetails.remove(this)
  }
}
