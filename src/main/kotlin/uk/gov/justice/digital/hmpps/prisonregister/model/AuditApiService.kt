package uk.gov.justice.digital.hmpps.prisonregister.model

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class AuditApiService(
  @Qualifier("auditWebClient") val auditWebClient: WebClient
) {
  fun auditPrisonEvent(auditEvent: AuditEvent) {
    auditWebClient.post().uri("/audit")
      .bodyValue(auditEvent)
      .retrieve()
      .toBodilessEntity()
      .block()
  }
}
