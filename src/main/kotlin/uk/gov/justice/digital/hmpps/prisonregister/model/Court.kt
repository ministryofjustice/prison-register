package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Transient
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Persistable
import java.time.LocalDate

@Entity
data class Court(
  @Id
  @Column(unique = true)
  val courtId: String,
  var name: String,
  var description: String?,
  var active: Boolean,
  var inactiveDate: LocalDate?,
  var cjitCode: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "area")
  var area: Area?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region")
  var region: Region?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "court_type")
  var courtType: CourtType,

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "court_to_agency_address",
    joinColumns = [JoinColumn(name = "court_id")],
    inverseJoinColumns = [JoinColumn(name = "agency_address_id", referencedColumnName = "id")],
  )
  var addresses: MutableList<AgencyAddress> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "court_to_email_address",
    joinColumns = [JoinColumn(name = "court_id")],
    inverseJoinColumns = [JoinColumn(name = "email_address_id", referencedColumnName = "id")],
  )
  var emailAddresses: MutableList<EmailAddress> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "court_to_phone",
    joinColumns = [JoinColumn(name = "court_id")],
    inverseJoinColumns = [JoinColumn(name = "phone_id", referencedColumnName = "id")],
  )
  var phoneNumbers: MutableList<PhoneNumber> = mutableListOf(),

  @Transient
  @Value("false")
  val new: Boolean = true,

) : Persistable<String> {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Court

    return courtId == other.courtId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(courtId = $courtId, name = $name, description = $description"
  override fun getId(): String? = courtId
  override fun isNew(): Boolean = new
}
