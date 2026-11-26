package uk.gov.justice.digital.hmpps.prisonregister.loadtest

enum class Profile {
  /** 5 requests a second for a minute, to check the set-up before a real run */
  SMOKE,

  /** Ramp up to MULTIPLIER times the peak and hold it. Has the MAPB-1129 pass marks. */
  STEADY,

  /** Step up one peak at a time to MAX_MULTIPLIER times the peak, to find where it breaks */
  STRESS,
}

/**
 * Settings, read from environment variables. The defaults point at preprod and run the smoke
 * profile, so nothing heavy happens by accident.
 */
data class LoadTestSettings(
  val baseUrl: String,
  val authUrl: String,
  val clientId: String?,
  val clientSecret: String?,
  val authToken: String?,
  val profile: Profile,
  val peakPerMinute: Double,
  val multiplier: Double,
  val rampMinutes: Long,
  val holdMinutes: Long,
  val maxMultiplier: Int,
  val stepMinutes: Long,
  val includeSync: Boolean,
) {
  fun describe(totalShare: Double): String {
    val load = when (profile) {
      Profile.SMOKE -> "5 requests a second for 1 minute"
      Profile.STEADY -> "ramp to ${perSecond(multiplier)} requests a second (${multiplier}x a peak of ${peakPerMinute.toInt()} a minute) over $rampMinutes minutes, then hold for $holdMinutes minutes"
      Profile.STRESS -> "step up by ${perSecond(1.0)} requests a second every $stepMinutes minutes, to ${perSecond(maxMultiplier.toDouble())} (${maxMultiplier}x the peak)"
    }
    return """
      |Prison register load test
      |  Target:  $baseUrl
      |  Profile: $profile - $load
      |  NOMIS reconciliation calls: ${if (includeSync) "included" else "left out"} (shares add up to ${"%.2f".format(totalShare)}%, scaled to 100%)
    """.trimMargin()
  }

  private fun perSecond(times: Double) = "%.0f".format(peakPerMinute * times / 60)

  companion object {
    // Highest minute in prod for the register plus the Prison API /api/agencies calls that will
    // move to it, 30 days to 13 November 2026
    private const val PROD_PEAK_PER_MINUTE = 10_500.0

    fun fromEnvironment(env: Map<String, String> = System.getenv()): LoadTestSettings = LoadTestSettings(
      baseUrl = env["BASE_URL"] ?: "https://prison-register-preprod.hmpps.service.justice.gov.uk",
      authUrl = env["AUTH_URL"] ?: "https://sign-in-preprod.hmpps.service.justice.gov.uk/auth/oauth/token",
      clientId = env["CLIENT_ID"],
      clientSecret = env["CLIENT_SECRET"],
      authToken = env["AUTH_TOKEN"],
      profile = Profile.valueOf((env["PROFILE"] ?: "smoke").uppercase()),
      peakPerMinute = env["PEAK_PER_MINUTE"]?.toDouble() ?: PROD_PEAK_PER_MINUTE,
      multiplier = env["MULTIPLIER"]?.toDouble() ?: 2.0,
      rampMinutes = env["RAMP_MINUTES"]?.toLong() ?: 5,
      holdMinutes = env["HOLD_MINUTES"]?.toLong() ?: 20,
      maxMultiplier = env["MAX_MULTIPLIER"]?.toInt() ?: 5,
      stepMinutes = env["STEP_MINUTES"]?.toLong() ?: 3,
      includeSync = env["INCLUDE_SYNC"].toBoolean(),
    )
  }
}
