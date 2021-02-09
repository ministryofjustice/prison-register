package uk.gov.justice.digital.hmpps.courtregister.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id

@Repository
interface CourtRepository : CrudRepository<Court, String> {
  fun findByActiveOrderById(active: Boolean): List<Court>
}

@Entity
data class Court(
  @Id
  val id: String,
  val courtName: String,
  val courtDescription: String?,
  val active: Boolean
)
