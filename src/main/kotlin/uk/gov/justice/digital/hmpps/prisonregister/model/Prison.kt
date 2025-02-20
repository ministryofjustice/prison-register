package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.OneToMany
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.prisonregister.resource.UpdateAddressDto
import java.time.LocalDate

@Entity
@NamedEntityGraph(
  name = "prison-entity-graph",
  attributeNodes = [
    NamedAttributeNode("prisonTypes"),
    NamedAttributeNode("categories"),
    NamedAttributeNode("addresses"),
    NamedAttributeNode("prisonOperators"),
  ],
)
data class Prison(
  @Id
  @Column(unique = true)
  val prisonId: String,
  var name: String,
  var description: String? = null,
  var active: Boolean,
  var male: Boolean = false,
  var female: Boolean = false,
  var contracted: Boolean = false,
  var lthse: Boolean = false,
  var prisonNameInWelsh: String? = null,

  var inactiveDate: LocalDate? = null,

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var prisonTypes: MutableSet<PrisonType> = mutableSetOf(),

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
    name = "prison_category",
    joinColumns = [JoinColumn(name = "prison_id")],
  )
  @Column(name = "category")
  @Enumerated(EnumType.STRING)
  var categories: MutableSet<Category> = mutableSetOf(),

  @OneToMany
  @JoinTable(
    name = "PRISON_OPERATOR",
    joinColumns = [JoinColumn(name = "prison_id")],
    inverseJoinColumns = [JoinColumn(name = "operator_id", referencedColumnName = "id")],
  )
  var prisonOperators: Set<Operator> = setOf(),

  @OneToMany(mappedBy = "prison", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var addresses: Set<Address> = setOf(),

) {

  @Column(name = "gp_practice_code", nullable = true)
  var gpPractice: String? = null

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

    return prisonId == other.prisonId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(prisonId = $prisonId, name = $name, description = $description, active = $active," +
      " male = $male, female = $female, contracted = $contracted, lthse = $lthse, categories = $categories )"
  }
}

enum class Gender(val columnName: String) {
  MALE("male"),
  FEMALE("female"),
}

enum class Category { A, B, C, D }
