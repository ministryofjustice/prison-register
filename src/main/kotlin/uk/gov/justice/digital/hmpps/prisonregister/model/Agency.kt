package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
data class Agency(
  @Id
  @Column(unique = true)
  val agencyId: String,
  var name: String,
  var description: String?,
  var active: Boolean,
  @Enumerated(EnumType.STRING)
  var accessibleAccess: AccessibleAccess?,
  @Enumerated(EnumType.STRING)
  var agencyType: AgencyType,
  var inactiveDate: LocalDate?,
  var cjitCode: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "area")
  var area: Area?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "region")
  var region: Region?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "geographical_area")
  var geographicalArea: Area?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payroll_region")
  var payrollRegion: PayrollRegion?,

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "agency_to_agency_address",
    joinColumns = [JoinColumn(name = "agency_id")],
    inverseJoinColumns = [JoinColumn(name = "agency_address_id", referencedColumnName = "id")],
  )
  var addresses: MutableList<AgencyAddress> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "agency_to_email_address",
    joinColumns = [JoinColumn(name = "agency_id")],
    inverseJoinColumns = [JoinColumn(name = "email_address_id", referencedColumnName = "id")],
  )
  var emailAddresses: MutableList<EmailAddress> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinTable(
    name = "agency_to_phone",
    joinColumns = [JoinColumn(name = "agency_id")],
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
    other as Agency

    return agencyId == other.agencyId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(agencyId = $agencyId, name = $name, description = $description"
  override fun getId(): String? = agencyId
  override fun isNew(): Boolean = new
}

enum class AgencyType {
  AIRPORT,
  PROBATION_CRC,
  FOREIGN_NATIONAL_PRISON,
  VOLUNTARY_HOSTEL,
  IMMIGRATION_DETENTION_CENTRE,
  OUTSIDE,
  PECS,
  PSYCHIATRIC_CARE,
  CHILDREN_SECURE_HOME,
  SECURE_TRAINING_CENTRE,
  YOT,
}
