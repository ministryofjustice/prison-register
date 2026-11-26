package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionExecution
import org.springframework.transaction.TransactionExecutionListener

const val PRISONS_CACHE = "prisons"
const val PRISON_NAMES_CACHE = "prison-names"
const val AGENCY_CACHE = "agency"
const val AGENCIES_CACHE = "agencies"
const val AGENCIES_BY_TYPE_CACHE = "agencies-by-type"

/**
 * In-memory caches for the busy read endpoints (MAPB-1128). The caches, their size and expiry are
 * set under spring.cache in application.yml.
 *
 * Each pod has its own caches. A pod clears them all as soon as it commits a change to the register
 * (see [ClearCachesAfterChange]). Other pods go on serving the old version until their entries
 * expire.
 *
 * The cache check runs before the transaction starts, so a cache hit never opens a transaction or
 * takes a database connection.
 */
@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
class CacheConfiguration

/**
 * Clears every cache after any read-write transaction commits, so a change made through the
 * maintenance endpoints or the NOMIS sync is seen by the next read on this pod.
 *
 * Changes are rare (a few a week), so clearing everything is simpler and safer than working out
 * which entries a change affects. Read-only transactions do not clear the caches, so read methods
 * should be marked @Transactional(readOnly = true).
 */
@Component
class ClearCachesAfterChange(private val cacheManager: CacheManager) : TransactionExecutionListener {
  override fun afterCommit(transaction: TransactionExecution, commitFailure: Throwable?) {
    if (!transaction.isReadOnly) {
      cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
    }
  }
}
