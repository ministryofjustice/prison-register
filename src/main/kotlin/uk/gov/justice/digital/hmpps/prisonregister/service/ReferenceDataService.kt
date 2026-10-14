package uk.gov.justice.digital.hmpps.prisonregister.service

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonregister.model.AreaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.CountySuggestionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.LocalAuthorityRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.PayrollRegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.RegionRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.SubareaRepository
import uk.gov.justice.digital.hmpps.prisonregister.model.TownSuggestionRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.CodeDescription

@Service
@Transactional(readOnly = true)
class ReferenceDataService(
  private val countySuggestionRepository: CountySuggestionRepository,
  private val townSuggestionRepository: TownSuggestionRepository,
  private val areaRepository: AreaRepository,
  private val regionRepository: RegionRepository,
  private val payrollRegionRepository: PayrollRegionRepository,
  private val localAuthorityRepository: LocalAuthorityRepository,
  private val subareaRepository: SubareaRepository,
) {
  fun getAreas(): List<CodeDescription> = areaRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getAreasByRegion(region: String): List<CodeDescription> = areaRepository.findAllByRegionCode(region, Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getRegions(): List<CodeDescription> = regionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getPayrollRegions(): List<CodeDescription> = payrollRegionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getLocalAuthorities(): List<CodeDescription> = localAuthorityRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getSubareasByArea(area: String): List<CodeDescription> = subareaRepository.findAllByAreaCode(area, Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getCountySuggestions(): List<CodeDescription> = countySuggestionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }

  fun getTownSuggestions(): List<CodeDescription> = townSuggestionRepository.findAll(Sort.by("description"))
    .map { CodeDescription(code = it.code, description = it.description) }
}
