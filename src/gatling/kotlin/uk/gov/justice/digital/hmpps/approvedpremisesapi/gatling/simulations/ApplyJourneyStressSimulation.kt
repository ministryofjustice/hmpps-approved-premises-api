package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.simulations

import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.repeat
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.CoreDsl.stressPeakUsers
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.authorizeUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.toJson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.withAuthorizedUserHttpProtocol
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class ApplyJourneyStressSimulation : Simulation() {
  private val createTemporaryAccommodationApplication = exec(
    http("Create Application")
      .post("/applications")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .body(
        toJson(
          NewApplication(
            crn = "X320741",
            convictionId = 0L,
            deliusEventNumber = "",
            offenceId = "",
          ),
        ),
      )
      .check(status().`is`(201))
      .check(jsonPath("$.id").saveAs("application_id")),
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val updateTemporaryAccommodationApplication = repeat({ randomInt(1, 20) }, "n").on(
    exec(
      http("Update Application")
        .put("/applications/#{application_id}")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .body(
          toJson(
            UpdateTemporaryAccommodationApplication(
              type = "CAS3",
              data = mapOf(),
            ),
          ),
        ),
    ).pause(5.seconds.toJavaDuration()),
  )

  private val submitTemporaryAccommodationApplication = exec(
    http("Submit Application")
      .post("/applications/#{application_id}/submission")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .body(
        toJson(
          SubmitTemporaryAccommodationApplication(
            type = "CAS3",
            translatedDocument = "{}",
          ),
        ),
      ),
  ).pause(10.seconds.toJavaDuration())

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
    ).withAuthorizedUserHttpProtocol()
  }
}
