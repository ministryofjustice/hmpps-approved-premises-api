package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.simulations.cas2

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.CoreDsl.stressPeakUsers
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas2.createCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas2.submitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas2.updateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas2.viewAllMyCas2Applications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.authorizeUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.getUUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.withAuthorizedUserHttpProtocol
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ApplyJourneyStressSimulation : Simulation() {
  private val applicationIdKey = "application_id"
  private val getApplicationId = { session: Session -> session.getUUID(applicationIdKey) }

  private val createCas2Application = createCas2Application(
    saveApplicationIdAs = applicationIdKey,
  ).exitHereIfFailed()
    .pause(
      1.seconds.toJavaDuration(),
    )

  private val updateCas2Application = repeat({ randomInt(1, 20) }, "n").on(
    updateCas2Application(
      applicationId = getApplicationId,
    )
      .pause(
        5.seconds.toJavaDuration(),
      ),
  )

  private val submitCas2Application = submitCas2Application(
    applicationId = getApplicationId,
  ).pause(
    10.seconds.toJavaDuration(),
  )

  private val viewAllMyCas2Applications = viewAllMyCas2Applications()
    .pause(10.seconds.toJavaDuration())

  private val cas2ApplyJourney = scenario("Apply journey for CAS2")
    .exec(
      authorizeUser("cas2"),
      createCas2Application,
      updateCas2Application,
      submitCas2Application,
      viewAllMyCas2Applications,
    )

  init {
    setUp(
      cas2ApplyJourney.injectOpen(
        constantUsersPerSec(50.0).during(2.minutes.toJavaDuration()).randomized(),
        stressPeakUsers(5000).during(1.minutes.toJavaDuration()),
      ),
    ).assertions(
      CoreDsl.global().responseTime().percentile(95.0).lt(40000),
      CoreDsl.global().successfulRequests().percent().gte(100.0),
    ).withAuthorizedUserHttpProtocol()
  }
}
