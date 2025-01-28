package uk.gov.justice.digital.hmpps.prisonregister.utils

import org.springframework.stereotype.Component
import java.lang.ThreadLocal

@Component
object UserContext {
  private val authToken = ThreadLocal<String>()
  fun getAuthToken() = authToken.get()
  fun setAuthToken(aToken: String?) = authToken.set(aToken)
}
