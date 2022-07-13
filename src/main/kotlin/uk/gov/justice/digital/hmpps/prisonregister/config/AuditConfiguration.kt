package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Service
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class AuditConfiguration

@Service(value = "auditorAware")
class AuditorAwareImpl(private val securityUserContext: SecurityUserContext) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> {
    return Optional.ofNullable(securityUserContext.principal)
  }
}
