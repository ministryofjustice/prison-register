package uk.gov.justice.digital.hmpps.prisonregister.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.prisonregister.config.PRISONS_CACHE
import uk.gov.justice.digital.hmpps.prisonregister.config.PRISON_NAMES_CACHE
import uk.gov.justice.digital.hmpps.prisonregister.model.PrisonRepository
import uk.gov.justice.digital.hmpps.prisonregister.resource.PrisonDto
import uk.gov.justice.digital.hmpps.prisonregister.resource.dto.PrisonNameDto

/**
 * Serves the busy public prison reads (GET /prisons, /prisons/id/{prisonId}, /prisons/names and
 * POST /prisons/prisonsByIds) from an in-memory copy of the prisons (MAPB-1128).
 *
 * Every prison is held in one cache entry, so any mix of ids sent to prisonsByIds is served from
 * the cache. Prison names are cached separately because they are sorted by the database.
 *
 * This class is deliberately not @Transactional: a cache hit must not take a database connection.
 * The database is only read, in a read-only transaction, when the cache is empty.
 */
@Service
class PrisonLookupService(
  private val prisonRepository: PrisonRepository,
  cacheManager: CacheManager,
  transactionManager: PlatformTransactionManager,
) {
  private val prisonsCache = cacheManager.getCache(PRISONS_CACHE)!!
  private val prisonNamesCache = cacheManager.getCache(PRISON_NAMES_CACHE)!!
  private val readOnlyTransaction = TransactionTemplate(transactionManager).apply { isReadOnly = true }

  fun findById(prisonId: String): PrisonDto = allPrisons().byId[prisonId] ?: throw EntityNotFoundException("Prison $prisonId not found")

  fun findAll(): List<PrisonDto> = allPrisons().prisons

  fun findByIds(prisonIds: Collection<String>): List<PrisonDto> {
    val wanted = prisonIds.toSet()
    return allPrisons().prisons.filter { it.prisonId in wanted }
  }

  fun getPrisonNames(active: Boolean?, prisonId: String?): List<PrisonNameDto> = prisonNames(active).filter { prisonId == null || it.prisonId == prisonId }

  private fun allPrisons(): AllPrisons = prisonsCache.get("all") {
    readOnlyTransaction.execute { AllPrisons(prisonRepository.findAll().map { PrisonDto(it) }) }
  }!!

  private fun prisonNames(active: Boolean?): List<PrisonNameDto> = prisonNamesCache.get(active.toString()) {
    readOnlyTransaction.execute {
      prisonRepository.findByActiveOrderByPrisonName(active = active).map { PrisonNameDto(it.prisonId, it.name, it.prisonNameInWelsh) }
    }
  }!!

  private class AllPrisons(val prisons: List<PrisonDto>) {
    val byId: Map<String, PrisonDto> = prisons.associateBy { it.prisonId }
  }
}
