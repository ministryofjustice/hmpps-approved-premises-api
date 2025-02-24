package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.simulations

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.CoreDsl.stressPeakUsers
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.createTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.submitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.updateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.authorizeUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.getUUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.withAuthorizedUserHttpProtocol
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Deprecated(message = "No longer used as migrated Performance tests to hmpps-load-testing-probation")
@Suppress("MagicNumber")
class ApplyJourneyStressSimulation : Simulation() {
  private val applicationIdKey = "application_id"
  private val getApplicationId = { session: Session -> session.getUUID(applicationIdKey) }

  private val createTemporaryAccommodationApplication = createTemporaryAccommodationApplication(
    saveApplicationIdAs = applicationIdKey,
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val updateTemporaryAccommodationApplication = repeat({ randomInt(1, 20) }, "n").on(
    updateTemporaryAccommodationApplication(
      applicationId = getApplicationId,
    )
      .pause(5.seconds.toJavaDuration()),
  )

  private val submitTemporaryAccommodationApplication = submitTemporaryAccommodationApplication(
    applicationId = getApplicationId,
  )
    .pause(10.seconds.toJavaDuration())

  private val temporaryAccommodationApplyJourney = scenario("Apply journey for Temporary Accommodation")
    .exec(
      authorizeUser(),
      createTemporaryAccommodationApplication,
      updateTemporaryAccommodationApplication,
      submitTemporaryAccommodationApplication,
    )

  init {
    setUp(
      temporaryAccommodationApplyJourney.injectOpen(
        constantUsersPerSec(50.0).during(2.minutes.toJavaDuration()).randomized(),
        stressPeakUsers(5000).during(1.minutes.toJavaDuration()),
      ),
    )
      .assertions(
        CoreDsl.global().responseTime().percentile(95.0).lt(40000),
        CoreDsl.global().successfulRequests().percent().gte(100.0),
      )
      .withAuthorizedUserHttpProtocol()
  }
}
