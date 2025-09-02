package uk.gov.justice.digital.hmpps.approvedpremisesapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

const val SYSTEM_USERNAME = "COMMUNITY_ACCOMMODATION_API"

@SpringBootApplication
class Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<Application>(*args)
}
