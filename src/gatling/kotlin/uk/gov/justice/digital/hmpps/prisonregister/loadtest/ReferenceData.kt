package uk.gov.justice.digital.hmpps.prisonregister.loadtest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.random.Random

/**
 * The prison and agency ids the test asks for, read from the environment under test before the
 * run starts, so the test does not depend on a fixed list.
 */
class ReferenceData(
  private val prisonIds: List<String>,
  private val otherAgencyIds: List<String>,
) {
  /** An active prison */
  fun prisonId(): String = prisonIds.random()

  /** One to five active prisons, for prisonsByIds */
  fun somePrisonIds(): List<String> = List(Random.nextInt(1, 6)) { prisonId() }.distinct()

  /**
   * Most single agency lookups in prod are for prisons: of the 305 ids asked for, the 30 busiest
   * are all prisons. So 9 in 10 lookups here are for a prison and the rest for any other agency.
   */
  fun agencyId(): String = if (otherAgencyIds.isEmpty() || Random.nextInt(10) < 9) prisonId() else otherAgencyIds.random()

  /** Any active agency, as the NOMIS reconciliation works through every agency in turn */
  fun anyAgencyId(): String = if (Random.nextInt(prisonIds.size + otherAgencyIds.size) < prisonIds.size) prisonId() else otherAgencyIds.random()

  companion object {
    private val mapper = ObjectMapper()

    fun load(settings: LoadTestSettings, token: TokenSource): ReferenceData {
      val client = HttpClient.newHttpClient()
      fun get(path: String, authorised: Boolean = false): JsonNode {
        val request = HttpRequest.newBuilder(URI.create("${settings.baseUrl}$path"))
          .header("Accept", "application/json")
          .apply { if (authorised) header("Authorization", "Bearer ${token.current()}") }
          .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "Could not read $path from ${settings.baseUrl}: HTTP ${response.statusCode()}" }
        return mapper.readTree(response.body())
      }

      val prisonIds = get("/prisons").filter { it.path("active").asBoolean() }.map { it.path("prisonId").asText() }
      val otherAgencyIds = get("/api/agencies", authorised = true)
        .filter { it.path("agencyType").asText() != "PRISON" }
        .map { it.path("agencyId").asText() }
      check(prisonIds.isNotEmpty()) { "No active prisons found at ${settings.baseUrl}" }
      println("Using ${prisonIds.size} active prisons and ${otherAgencyIds.size} other active agencies from ${settings.baseUrl}")
      return ReferenceData(prisonIds, otherAgencyIds)
    }
  }
}
