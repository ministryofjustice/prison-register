package uk.gov.justice.digital.hmpps.prisonregister.model

import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
data class Prison(
  @Id
  val prisonId: String,
  var name: String,
  var description: String? = null,
  var active: Boolean,
  var male: Boolean = false,
  var female: Boolean = false,

  var inactiveDate: LocalDate? = null,

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var prisonTypes: MutableSet<PrisonType> = mutableSetOf(),

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var addresses: List<Address> = listOf(),
) {

  @OneToOne
  @JoinColumn(name = "prison_id")
  var gpPractice: PrisonGpPractice? = null

  fun addAddress(dto: UpdateAddressDto): Address {
    val building = Address(
      prison = this,
      addressLine1 = dto.addressLine1,
      addressLine2 = dto.addressLine2,
      town = dto.town,
      county = dto.county,
      postcode = dto.postcode,
      country = dto.country,
    )
    addresses = addresses.plus(building)

    return building
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Prison

    return prisonId != null && prisonId == other.prisonId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(prisonId = $prisonId , name = $name , description = $description , active = $active ," +
      " male = $male, female = $female )"
  }
}

enum class Gender(val columnName: String) {
  MALE("male"),
  FEMALE("female"),
}
