package uk.gov.justice.digital.hmpps.approvedpremisesapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<Application>(*args)
}
