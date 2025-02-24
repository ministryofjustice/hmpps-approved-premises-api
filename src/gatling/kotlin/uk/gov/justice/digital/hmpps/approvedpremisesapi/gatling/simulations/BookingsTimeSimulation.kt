package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.simulations

import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.createTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.createTemporaryAccommodationBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.createTemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.createTemporaryAccommodationRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.getProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.getUserProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.submitTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.updateTemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.authorizeUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.getUUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.withAuthorizedUserHttpProtocol
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAround
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Deprecated(message = "No longer used as migrated Performance tests to hmpps-load-testing-probation")
@Suppress("MagicNumber")
class BookingsTimeSimulation : Simulation() {
  private val arrivalDateKey = "arrival_date"
  private val getArrivalDate = { session: Session -> session.get<LocalDate>(arrivalDateKey)!! }

  private val reportDateKey = "report_date"

  private val probationRegionIdKey = "probation_region_id"
  private val getProbationRegionId = { session: Session -> session.getUUID(probationRegionIdKey) }

  private val probationDeliveryUnitIdKey = "probation_delivery_unit_id"
  private val getProbationDeliveryUnitId = { session: Session -> session.getUUID(probationDeliveryUnitIdKey) }

  private val premisesIdKey = "premises_id"
  private val getPremisesId = { session: Session -> session.getUUID(premisesIdKey) }

  private val bedIdKey = "bed_id"
  private val getBedId = { session: Session -> session.getUUID(bedIdKey) }

  private val applicationIdKey = "application_id"
  private val getApplicationId = { session: Session -> session.getUUID(applicationIdKey) }

  private val createArrivalDate = exec { session ->
    session.set(arrivalDateKey, LocalDate.now().randomDateAround(60))
  }

  private val createReportDate = exec { session ->
    session.set(reportDateKey, LocalDate.now().randomDateAround(60))
  }

  private val getUserProbationRegion = getUserProbationRegion(probationRegionIdKey)
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val getProbationDeliveryUnit = getProbationDeliveryUnit(
    probationRegionId = getProbationRegionId,
    saveProbationDeliveryUnitIdAs = probationDeliveryUnitIdKey,
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val createTemporaryAccommodationPremises = createTemporaryAccommodationPremises(
    probationRegionId = getProbationRegionId,
    probationDeliveryUnitId = getProbationDeliveryUnitId,
    savePremisesIdAs = premisesIdKey,
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val createTemporaryAccommodationRoom = createTemporaryAccommodationRoom(
    premisesId = getPremisesId,
    saveBedIdAs = bedIdKey,
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val createTemporaryAccommodationApplication = createTemporaryAccommodationApplication(
    saveApplicationIdAs = applicationIdKey,
  )
    .pause(1.seconds.toJavaDuration())

  private val updateTemporaryAccommodationApplication = updateTemporaryAccommodationApplication(
    applicationId = getApplicationId,
  )
    .pause(1.seconds.toJavaDuration())

  private val submitTemporaryAccommodationApplication = submitTemporaryAccommodationApplication(
    applicationId = getApplicationId,
    arrivalDate = getArrivalDate,
  )
    .pause(1.seconds.toJavaDuration())

  private val createTemporaryAccommodationBooking = createTemporaryAccommodationBooking(
    bedId = getBedId,
    arrivalDate = getArrivalDate,
  )
    .exitHereIfFailed()
    .pause(1.seconds.toJavaDuration())

  private val setupBooking = scenario("Setup booking")
    .exec(
      authorizeUser(),
      createArrivalDate,
      getUserProbationRegion,
      getProbationDeliveryUnit,
      createTemporaryAccommodationPremises,
      createTemporaryAccommodationRoom,
      createTemporaryAccommodationApplication,
      updateTemporaryAccommodationApplication,
      submitTemporaryAccommodationApplication,
      createTemporaryAccommodationBooking,
    )

  private val downloadBookingsReport = exec(
    http("Download Booking Report")
      .get("/reports/bookings/")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .queryParam("year") { session -> session.get<LocalDate>(reportDateKey)!!.year }
      .queryParam("month") { session -> session.get<LocalDate>(reportDateKey)!!.monthValue }
      .queryParam("probationRegionId", getProbationRegionId)
      .check(status().`is`(200)),
  )
    .pause(1.seconds.toJavaDuration())

  private val bookingSearch = exec(
    http("Bookings Search")
      .get("/bookings/search")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .queryParam("page", 1)
      .check(status().`is`(200)),
  )

  private val bookingsSearchJourney = scenario("Bookings search journey")
    .exec(
      authorizeUser(),
      bookingSearch,
    )

  private val bookingsReportDownloadJourney = scenario("Bookings report journey")
    .exec(
      authorizeUser(),
      createReportDate,
      getUserProbationRegion,
      downloadBookingsReport,
    )

  init {
    setUp(
      setupBooking.injectOpen(
        constantUsersPerSec(20.0).during(60.seconds.toJavaDuration()),
      )
        .andThen(
          bookingsSearchJourney.injectOpen(
            constantUsersPerSec(2.0).during(10.seconds.toJavaDuration()),
          ),
        )
        .andThen(
          bookingsReportDownloadJourney.injectOpen(
            constantUsersPerSec(0.25).during(400.seconds.toJavaDuration()),
          ),
        ),
    )
      .assertions(
        global().responseTime().percentile(95.0).lt(20000),
        global().successfulRequests().percent().gte(100.0),
      )
      .withAuthorizedUserHttpProtocol()
  }
}
