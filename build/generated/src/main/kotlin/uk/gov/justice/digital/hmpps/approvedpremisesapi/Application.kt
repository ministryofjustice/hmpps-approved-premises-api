package uk.gov.justice.digital.hmpps.approvedpremisesapi

import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["uk.gov.justice.digital.hmpps.approvedpremisesapi", "uk.gov.justice.digital.hmpps.approvedpremisesapi.api", "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
