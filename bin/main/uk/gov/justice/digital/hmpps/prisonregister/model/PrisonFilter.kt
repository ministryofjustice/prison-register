package uk.gov.justice.digital.hmpps.prisonregister.model

import com.google.common.collect.ImmutableList
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.lang.Nullable

class PrisonFilter(
  val active: Boolean? = null,
  val lthse: Boolean? = null,
  val textSearch: String? = null,
  val genders: List<Gender>? = listOf(),
  val prisonTypeCodes: List<Type>? = listOf(),
) : Specification<Prison> {

  override fun toPredicate(root: Root<Prison>, @Nullable query: CriteriaQuery<*>?, cb: CriteriaBuilder): Predicate? {
    val andBuilder = ImmutableList.builder<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>("active"), it))
    }
    lthse?.let {
      andBuilder.add(cb.equal(root.get<Any>("lthse"), it))
    }
    genders?.forEach {
      andBuilder.add(cb.equal(root.get<Any>(it.columnName), true))
    }
    if (!prisonTypeCodes.isNullOrEmpty()) {
      val prisonTypesPredicate =
        root.join<Any, Any>("prisonTypes").get<Any>("type").`in`(prisonTypeCodes.map { it })
      andBuilder.add(prisonTypesPredicate)
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = ImmutableList.builder<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, "prisonId"))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, "name"))
      andBuilder.add(cb.or(*orBuilder.build().toTypedArray()))
    }
    query?.orderBy(cb.asc(root.get<Any>("prisonId")))
    query?.distinct(true)
    return cb.and(*andBuilder.build().toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<Prison>,
    cb: CriteriaBuilder,
    field: String,
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
