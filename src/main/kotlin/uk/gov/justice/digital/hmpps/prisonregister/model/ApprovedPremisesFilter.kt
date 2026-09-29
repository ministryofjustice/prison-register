package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

class ApprovedPremisesFilter(
  val active: Boolean? = null,
  val textSearch: String? = null,
) : Specification<ApprovedPremises> {

  override fun toPredicate(root: Root<ApprovedPremises>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
    val andBuilder = mutableListOf<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>(ApprovedPremises::active.name), it))
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = mutableListOf<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, ApprovedPremises::approvedPremisesId.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, ApprovedPremises::name.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, ApprovedPremises::description.name))
      andBuilder.add(cb.or(*orBuilder.toTypedArray()))
    }
    query.orderBy(cb.asc(root.get<Any>(ApprovedPremises::approvedPremisesId.name)))
    query.distinct(true)
    return cb.and(*andBuilder.toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<ApprovedPremises>,
    cb: CriteriaBuilder,
    field: String,
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
