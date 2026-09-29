package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

class HospitalFilter(
  val active: Boolean? = null,
  val textSearch: String? = null,
  val highSecurity: Boolean? = null,
) : Specification<Hospital> {

  override fun toPredicate(root: Root<Hospital>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
    val andBuilder = mutableListOf<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>(Hospital::active.name), it))
    }
    highSecurity?.let {
      andBuilder.add(cb.equal(root.get<Any>(Hospital::highSecurity.name), it))
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = mutableListOf<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Hospital::hospitalId.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Hospital::name.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, Hospital::description.name))
      andBuilder.add(cb.or(*orBuilder.toTypedArray()))
    }
    query.orderBy(cb.asc(root.get<Any>(Hospital::hospitalId.name)))
    query.distinct(true)
    return cb.and(*andBuilder.toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<Hospital>,
    cb: CriteriaBuilder,
    field: String,
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
