package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
  @Value("\${audit.endpoint.url}") private val auditBaseUri: String
) {

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthRootUri).build()
  }

  @Bean
  fun auditWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("audit-api")
    return WebClient.builder()
      .baseUrl(auditBaseUri)
      .apply(oauth2Client.oauth2Configuration())
      .exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { configurer ->
            configurer.defaultCodecs()
              .maxInMemorySize(-1)
          }
          .build()
      )
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
