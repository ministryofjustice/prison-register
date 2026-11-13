package uk.gov.justice.digital.hmpps.prisonregister.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Supplies the bearer token for the endpoints that need one.
 *
 * Either takes AUTH_TOKEN as given, or gets a client credentials token from HMPPS Auth with
 * CLIENT_ID and CLIENT_SECRET and renews it every 10 minutes, because a run can last longer than
 * the token does.
 */
class TokenSource(private val settings: LoadTestSettings) : AutoCloseable {
  private val client = HttpClient.newHttpClient()
  private val token = AtomicReference<String>()
  private val renewer = Executors.newSingleThreadScheduledExecutor { Thread(it, "token-renewer").apply { isDaemon = true } }

  init {
    if (!settings.authToken.isNullOrBlank()) {
      token.set(settings.authToken)
    } else {
      require(!settings.clientId.isNullOrBlank() && !settings.clientSecret.isNullOrBlank()) {
        "Set AUTH_TOKEN, or CLIENT_ID and CLIENT_SECRET for a client with the HMPPS_REGISTERS_API__R role"
      }
      token.set(fetch())
      renewer.scheduleAtFixedRate({ token.set(fetch()) }, RENEW_MINUTES, RENEW_MINUTES, TimeUnit.MINUTES)
    }
  }

  fun current(): String = token.get()

  private fun fetch(): String {
    val credentials = Base64.getEncoder().encodeToString("${settings.clientId}:${settings.clientSecret}".toByteArray())
    val request = HttpRequest.newBuilder(URI.create("${settings.authUrl}?grant_type=client_credentials"))
      .header("Authorization", "Basic $credentials")
      .POST(HttpRequest.BodyPublishers.noBody())
      .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) { "Could not get a token from ${settings.authUrl}: HTTP ${response.statusCode()}" }
    return ObjectMapper().readTree(response.body()).path("access_token").asText()
  }

  override fun close() {
    renewer.shutdownNow()
  }

  private companion object {
    const val RENEW_MINUTES = 10L
  }
}
