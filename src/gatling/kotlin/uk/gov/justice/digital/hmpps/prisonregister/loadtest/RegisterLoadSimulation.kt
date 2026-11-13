package uk.gov.justice.digital.hmpps.prisonregister.loadtest

import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.details
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec
import io.gatling.javaapi.core.CoreDsl.rampUsersPerSec
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.OpenInjectionStep
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import io.gatling.javaapi.http.HttpRequestActionBuilder
import java.time.Duration

/**
 * Load test for the prison register (MAPB-1129).
 *
 * Sends the prod mix of calls at a multiple of the expected peak. The mix and the peak come from
 * 30 days of prod traffic to 13 November 2026: the register's own calls, plus the Prison API
 * /api/agencies calls that will move to the register. See docs/load-test.md for how to run it and
 * what the numbers are based on.
 *
 * Each endpoint is its own scenario with its own arrival rate, so the mix stays the same at every
 * load level and each endpoint is reported separately.
 */
class RegisterLoadSimulation : Simulation() {

  private val settings = LoadTestSettings.fromEnvironment()
  private val token = TokenSource(settings)
  private val referenceData = ReferenceData.load(settings, token)

  private val httpProtocol = http.baseUrl(settings.baseUrl)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("prison-register-load-test")
    // The register's callers are services with connection pools, so share connections rather
    // than open a new TLS connection for every request
    .shareConnections()
    .disableWarmUp()

  private fun withToken(request: HttpRequestActionBuilder): HttpRequestActionBuilder = request.header("Authorization") { _: Session -> "Bearer ${token.current()}" }

  private fun ok(request: HttpRequestActionBuilder): ChainBuilder = exec(request.check(status().shouldBe(200)))

  /**
   * One entry per endpoint. Shares are percentages of all calls, from prod (see docs/load-test.md).
   */
  private val endpoints: List<Endpoint> = listOfNotNull(
    // Today's register calls
    Endpoint(
      "prisons by ids",
      31.40,
      ok(
        http("prisons by ids").post("/prisons/prisonsByIds")
          .body(StringBody { _ -> """{"prisonIds":[${referenceData.somePrisonIds().joinToString(",") { "\"$it\"" }}]}""" })
          .asJson(),
      ),
    ),
    Endpoint("prison names (active)", 5.27, ok(http("prison names (active)").get("/prisons/names?active=true"))),
    Endpoint(
      "prison names (one prison)",
      2.03,
      ok(http("prison names (one prison)").get { _ -> "/prisons/names?prison_id=${referenceData.prisonId()}" }),
    ),
    Endpoint("prison names (all)", 0.69, ok(http("prison names (all)").get("/prisons/names"))),
    Endpoint("prison by id", 5.48, ok(http("prison by id").get { _ -> "/prisons/id/${referenceData.prisonId()}" })),
    Endpoint("all prisons", 0.76, ok(http("all prisons").get("/prisons"))),
    // Not every prison has social visit contact details, so a 404 is a normal answer
    Endpoint(
      "contact details",
      0.32,
      exec(
        withToken(
          http("contact details").get { _ -> "/secure/prisons/id/${referenceData.prisonId()}/department/contact-details?departmentType=SOCIAL_VISIT" },
        ).check(status().`in`(200, 404)),
      ),
    ),
    if (settings.includeSync) {
      Endpoint(
        "NOMIS reconciliation",
        0.33,
        ok(withToken(http("NOMIS reconciliation").get { _ -> "/legacy/reconciliation/${referenceData.anyAgencyId()}" })),
      )
    } else {
      null
    },

    // Calls moving from Prison API /api/agencies
    Endpoint("agency by id", 43.06, ok(withToken(http("agency by id").get { _ -> "/api/agencies/${referenceData.agencyId()}" }))),
    Endpoint("agencies by type (SCH)", 5.27, ok(withToken(http("agencies by type (SCH)").get("/api/agencies/legacy-type/SCH")))),
    Endpoint("agencies by type (INST)", 5.08, ok(withToken(http("agencies by type (INST)").get("/api/agencies/legacy-type/INST")))),
    Endpoint("agencies by type (HOSPITAL)", 0.07, ok(withToken(http("agencies by type (HOSPITAL)").get("/api/agencies/legacy-type/HOSPITAL")))),
    Endpoint("agencies by type (HSHOSP)", 0.07, ok(withToken(http("agencies by type (HSHOSP)").get("/api/agencies/legacy-type/HSHOSP")))),
  )

  // Without the reconciliation calls the shares no longer add up to 100, so scale them back up
  private val totalShare = endpoints.sumOf { it.share }

  private fun injection(endpoint: Endpoint): List<OpenInjectionStep> {
    val fraction = endpoint.share / totalShare
    val peakPerSecond = settings.peakPerMinute / 60.0 * fraction
    return when (settings.profile) {
      Profile.SMOKE -> listOf(
        atOnceUsers(1),
        constantUsersPerSec(SMOKE_REQUESTS_PER_SECOND * fraction).during(Duration.ofMinutes(1)),
      )
      Profile.STEADY -> {
        val target = peakPerSecond * settings.multiplier
        listOf(
          rampUsersPerSec(0.0).to(target).during(Duration.ofMinutes(settings.rampMinutes)),
          constantUsersPerSec(target).during(Duration.ofMinutes(settings.holdMinutes)),
        )
      }
      Profile.STRESS -> listOf(
        incrementUsersPerSec(peakPerSecond)
          .times(settings.maxMultiplier)
          .eachLevelLasting(Duration.ofMinutes(settings.stepMinutes))
          .separatedByRampsLasting(Duration.ofSeconds(30))
          .startingFrom(peakPerSecond),
      )
    }
  }

  private fun population(endpoint: Endpoint): PopulationBuilder = scenario(endpoint.name).exec(endpoint.chain).injectOpen(injection(endpoint))

  override fun before() {
    println(settings.describe(totalShare))
  }

  override fun after() {
    token.close()
  }

  init {
    val setUp = setUp(endpoints.map { population(it) }).protocols(httpProtocol)
    when (settings.profile) {
      Profile.SMOKE -> setUp.assertions(global().failedRequests().count().shouldBe(0L))
      Profile.STEADY -> setUp.assertions(
        // MAPB-1129 acceptance criteria
        global().failedRequests().percent().lt(0.1),
        details("agency by id").responseTime().percentile(95.0).lt(200),
      )
      // A stress run is expected to find the point where things fail, so it has no pass mark
      Profile.STRESS -> setUp
    }
  }

  private data class Endpoint(val name: String, val share: Double, val chain: ChainBuilder)

  private companion object {
    const val SMOKE_REQUESTS_PER_SECOND = 5.0
  }
}
