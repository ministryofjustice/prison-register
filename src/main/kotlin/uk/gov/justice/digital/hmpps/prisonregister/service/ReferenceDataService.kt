package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.CountySuggestionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.TownSuggestionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional(readOnly = true)
class ReferenceDataService(
  private val countySuggestionRepository: CountySuggestionRepository,
  private val townSuggestionRepository: TownSuggestionRepository,
) {
  fun getCountySuggestions(): List<CodeDescription> = countySuggestionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getTownSuggestions(): List<CodeDescription> = townSuggestionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }
}
