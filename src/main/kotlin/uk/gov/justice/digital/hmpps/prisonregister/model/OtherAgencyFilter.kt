package uk.gov.justice.digital.hmpps.prisonregister.model

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

class OtherAgencyFilter(
  val active: Boolean? = null,
  val textSearch: String? = null,
  val otherAgencyTypeCodes: List<OtherAgencyType>? = listOf(),
) : Specification<OtherAgency> {

  override fun toPredicate(root: Root<OtherAgency>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
    val andBuilder = mutableListOf<Predicate>()
    active?.let {
      andBuilder.add(cb.equal(root.get<Any>("active"), it))
    }
    if (!otherAgencyTypeCodes.isNullOrEmpty()) {
      val otherOtherAgencyTypesPredicate =
        root.get<Any>(OtherAgency::otherAgencyType.name).`in`(otherAgencyTypeCodes)
      andBuilder.add(otherOtherAgencyTypesPredicate)
    }
    if (!textSearch.isNullOrBlank()) {
      val orBuilder = mutableListOf<Predicate>()
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, OtherAgency::agencyId.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, OtherAgency::name.name))
      orBuilder.add(textSearchWildcardAndIgnoreCasePredicate(root, cb, OtherAgency::description.name))
      andBuilder.add(cb.or(*orBuilder.toTypedArray()))
    }
    query.orderBy(cb.asc(root.get<Any>(OtherAgency::agencyId.name)))
    query.distinct(true)
    return cb.and(*andBuilder.toTypedArray())
  }

  private fun textSearchWildcardAndIgnoreCasePredicate(
    root: Root<OtherAgency>,
    cb: CriteriaBuilder,
    field: String,
  ) = cb.like(cb.upper(root.get(field)), "%${textSearch?.uppercase()}%")
}
