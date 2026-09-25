package uk.gov.justice.digital.hmpps.prisonregister.dsl

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonregister.model.Prison
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import java.time.LocalDate

@DslMarker
annotation class PrisonDslMarker

@PrisonDslMarker
@Component
class PrisonBuilder(
  private val prisonRepository: PrisonRepository,
) {
  lateinit var prison: Prison
  fun build(
    prisonId: String,
    name: String,
    description: String?,
    active: Boolean,
    male: Boolean,
    female: Boolean,
    contracted: Boolean,
    lthse: Boolean,
    prisonNameInWelsh: String?,
    inactiveDate: LocalDate?,
  ): Prison = Prison(
    prisonId = prisonId,
    name = name,
    description = description,
    active = active,
    male = male,
    female = female,
    contracted = contracted,
    lthse = lthse,
    prisonNameInWelsh = prisonNameInWelsh,
    inactiveDate = inactiveDate,
  ).let {
    prisonRepository.saveAndFlush(it)
  }.also {
    prison = it
  }
}
