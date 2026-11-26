package uk.gov.justice.digital.hmpps.prisonregister.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Supplies the bearer token for the endpoints that need one.
 *
 * Either takes AUTH_TOKEN as given, or gets a client credentials token from HMPPS Auth with
 * CLIENT_ID and CLIENT_SECRET and renews it a minute before it expires, because a run can last
 * longer than the token does. Some clients' tokens last only a few minutes.
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
      renew()
    }
  }

  private fun renew() {
    val fresh = fetch()
    token.set(fresh)
    val renewInSeconds = (secondsLeft(fresh) - RENEW_BEFORE_EXPIRY_SECONDS).coerceAtLeast(MIN_RENEW_SECONDS)
    renewer.schedule({ renewSafely() }, renewInSeconds, TimeUnit.SECONDS)
  }

  // If HMPPS Auth fails, keep the current token and try again shortly rather than stop renewing
  private fun renewSafely() {
    try {
      renew()
    } catch (e: Exception) {
      System.err.println("Could not renew the token, trying again in $MIN_RENEW_SECONDS seconds: ${e.message}")
      renewer.schedule({ renewSafely() }, MIN_RENEW_SECONDS, TimeUnit.SECONDS)
    }
  }

  private fun secondsLeft(jwt: String): Long {
    val payload = String(Base64.getUrlDecoder().decode(jwt.split(".")[1]))
    return ObjectMapper().readTree(payload).path("exp").asLong() - Instant.now().epochSecond
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
    const val RENEW_BEFORE_EXPIRY_SECONDS = 60L
    const val MIN_RENEW_SECONDS = 15L
  }
}
