package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.simulations

import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.authorizeUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.withAuthorizedUserHttpProtocol
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class AssessmentsPageSizeSimulation : Simulation() {
  private val getAssessments = exec(
    http("List archived assessments")
      .get("/assessments")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .queryParam("statuses", "closed")
      .queryParam("statuses", "rejected")
      .queryParam("page", 1)
      .queryParam("perPage", 100)
      .check(status().`is`(200))
  )

  private val listArchivedAssessments = scenario("List archived assessments")
    .exec(
      authorizeUser(),
      getAssessments,
    )

  init {
    setUp(
      listArchivedAssessments.injectOpen(
        constantUsersPerSec(20.0).during(60.seconds.toJavaDuration()),
      )
    )
      .assertions(
        global().responseTime().percentile(95.0).lt(20000),
        global().successfulRequests().percent().gte(100.0),
      )
      .withAuthorizedUserHttpProtocol()
  }
}
