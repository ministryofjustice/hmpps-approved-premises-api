package uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.steps.cas3

import io.gatling.javaapi.core.CoreDsl
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.gatling.util.toJson
import java.time.LocalDate
import java.util.UUID

fun createTemporaryAccommodationBooking(
  bedId: (Session) -> UUID,
  crn: (Session) -> String = { _ -> CRN },
  arrivalDate: (Session) -> LocalDate = { _ -> LocalDate.now() },
  departureDate: (Session) -> LocalDate = { session -> arrivalDate(session).plusDays(84) },
  saveBookingIdAs: String? = null,
) = CoreDsl.exec(
  HttpDsl.http("Create Booking")
    .post("/premises/#{premises_id}/bookings")
    .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
    .body(
      toJson { session ->
        NewBooking(
          crn = crn(session),
          arrivalDate = arrivalDate(session),
          departureDate = departureDate(session),
          bedId = bedId(session),
          serviceName = ServiceName.temporaryAccommodation,
          enableTurnarounds = true,
          assessmentId = null,
        )
      },
    )
    .let {
      when (saveBookingIdAs) {
        null -> it
        else -> it.check(CoreDsl.jsonPath("$.id").saveAs(saveBookingIdAs))
      }
    },
)
