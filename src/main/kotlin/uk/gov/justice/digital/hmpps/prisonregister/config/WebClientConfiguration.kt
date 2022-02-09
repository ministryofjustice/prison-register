package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.prisonregister.utils.UserContext

@Configuration
class WebClientConfiguration(
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
  @Value("\${audit.endpoint.url}") val auditBaseUri: String

) {

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthRootUri).build()
  }

  @Bean
  fun auditWebClient(builder: WebClient.Builder): WebClient {
    return builder
      .baseUrl(auditBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }
  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest?, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
  }
}
