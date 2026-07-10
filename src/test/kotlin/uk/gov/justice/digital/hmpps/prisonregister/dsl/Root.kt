package uk.gov.justice.digital.hmpps.prisonregister.dsl

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.time.LocalDate

@DslMarker
annotation class DataDslMarker

@DataDslMarker
@Component
@Transactional
class Root(val courtBuilder: CourtBuilder) {
  fun court(
    courtId: String,
    name: String,
    description: String = name,
    active: Boolean = true,
    inactiveDate: LocalDate? = null,
    courtTypeCode: String = "CC",
    cjitCode: String? = null,
    areaCode: String? = null,
    regionCode: String? = null,
    dsl: CourtBuilder.() -> Unit,
  ): uk.gov.justice.digital.hmpps.prisonregister.model.Court = courtBuilder.build(
    courtId = courtId,
    name = name,
    description = description,
    active = active,
    inactiveDate = inactiveDate,
    courtTypeCode = courtTypeCode,
    cjitCode = cjitCode,
    areaCode = areaCode,
    regionCode = regionCode,
  ).also {
    dsl.invoke(courtBuilder)
  }
}
