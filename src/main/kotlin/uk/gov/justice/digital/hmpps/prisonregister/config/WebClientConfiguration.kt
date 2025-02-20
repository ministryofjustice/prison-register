package uk.gov.justice.digital.hmpps.prisonregister.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
) {

  @Bean
  fun oauthApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(oauthRootUri).build()
}
