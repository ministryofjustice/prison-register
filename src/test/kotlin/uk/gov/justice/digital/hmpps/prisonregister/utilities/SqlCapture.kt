package uk.gov.justice.digital.hmpps.prisonregister.utilities

import org.hibernate.cfg.AvailableSettings
import org.hibernate.resource.jdbc.spi.StatementInspector
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.util.concurrent.ConcurrentLinkedQueue

class SqlCapture : StatementInspector {
  val statements = ConcurrentLinkedQueue<String>()

  override fun inspect(sql: String): String = sql.also { statements.add(it) }
}

@TestConfiguration
class SqlCaptureConfiguration {
  @Bean
  fun sqlCapture() = SqlCapture()

  @Bean
  fun hibernatePropertiesCustomizer(sqlCapture: SqlCapture) = HibernatePropertiesCustomizer {
    it[AvailableSettings.STATEMENT_INSPECTOR] = sqlCapture
  }
}
