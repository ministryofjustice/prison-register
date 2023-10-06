package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "EMAIL_ADDRESS")
data class EmailAddress(
  @Column(name = "VALUE", nullable = false, unique = true)
  val value: String,

) : AbstractIdEntity() {

  @OneToMany(mappedBy = "emailAddress", fetch = FetchType.LAZY, cascade = [CascadeType.DETACH])
  val contactDetails: MutableList<ContactDetails> = mutableListOf()
}
