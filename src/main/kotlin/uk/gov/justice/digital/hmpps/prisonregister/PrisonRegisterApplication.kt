package uk.gov.justice.digital.hmpps.prisonregister

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrisonRegisterApplication

fun main(args: Array<String>) {
  runApplication<PrisonRegisterApplication>(*args)
}
