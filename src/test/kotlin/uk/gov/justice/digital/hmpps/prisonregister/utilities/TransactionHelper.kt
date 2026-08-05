package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class TransactionHelper {
  fun <T> runInTransaction(block: () -> T) = block()
}
