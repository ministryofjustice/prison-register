package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

class CourtFilter(
  val active: Boolean? = null,
  val textSearch: String? = null,
  val courtTypeCodes: List<CourtType.Companion.Type>? = listOf(),
) : Specification<Court> {

  override fun toPredicate(root: Root<Court>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
    val andBuilder = mutableListOf<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>("active"), it))
    }
    if (!courtTypeCodes.isNullOrEmpty()) {
      val courtTypesPredicate =
        root.join<Any, Any>(Court::courtType.name).get<Any>(CourtType::code.name).`in`(courtTypeCodes.map { it.code })
      andBuilder.add(courtTypesPredicate)
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = mutableListOf<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Court::courtId.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Court::name.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Court::description.name))
      andBuilder.add(cb.or(*orBuilder.toTypedArray()))
    }
    query.orderBy(cb.asc(root.get<Any>(Court::courtId.name)))
    query.distinct(true)
    return cb.and(*andBuilder.toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<Court>,
    cb: CriteriaBuilder,
    field: String,
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
