package uk.gov.justice.digital.hmpps.prisonregister.model

import com.google.common.collect.ImmutableList
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class PrisonFilter(
  val active: Boolean? = null,
  val textSearch: String? = null,
  val male: Boolean? = null,
  val female: Boolean? = null,
) : Specification<Prison> {

  override fun toPredicate(root: Root<Prison>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
    val andBuilder = ImmutableList.builder<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>("active"), it))
    }
    male?.let {
      andBuilder.add(cb.equal(root.get<Any>("male"), it))
    }
    female?.let {
      andBuilder.add(cb.equal(root.get<Any>("female"), it))
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = ImmutableList.builder<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, "prisonId"))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, "name"))
      andBuilder.add(cb.or(*orBuilder.build().toTypedArray()))
    }
    query.orderBy(cb.asc(root.get<Any>("prisonId")))
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<Prison>,
    cb: CriteriaBuilder,
    field: String
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
