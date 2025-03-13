package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import java.time.LocalDate
import java.time.LocalDateTime

const val TWO_WEEKS = 2L
const val SIX_WEEKS = 6L

@Component
class Cas1SpaceBookingStatusTransformer {

  fun transformToSpaceBookingSummaryStatus(
    spaceBookingDates: SpaceBookingDates,
  ): Cas1SpaceBookingSummaryStatus? {
    val nowDate = LocalDate.now()
    return when {
      spaceBookingDates.isNotArrived() -> Cas1SpaceBookingSummaryStatus.notArrived
      spaceBookingDates.hasDeparted() -> Cas1SpaceBookingSummaryStatus.departed
      spaceBookingDates.isOverdueDeparture(nowDate) -> Cas1SpaceBookingSummaryStatus.overdueDeparture
      spaceBookingDates.isDepartingToday(nowDate) -> Cas1SpaceBookingSummaryStatus.departingToday
      spaceBookingDates.isDepartingWithin2Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.departingWithin2Weeks
      spaceBookingDates.hasArrivedAndNotDeparted() -> Cas1SpaceBookingSummaryStatus.arrived
      spaceBookingDates.isArrivalToday(nowDate) -> Cas1SpaceBookingSummaryStatus.arrivingToday
      spaceBookingDates.isArrivalWithin2Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.arrivingWithin2Weeks
      spaceBookingDates.isArrivalWithin6Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.arrivingWithin6Weeks
      spaceBookingDates.isOverdueArrival(nowDate) -> Cas1SpaceBookingSummaryStatus.overdueArrival
      else -> null
    }
  }
}

data class SpaceBookingDates(
  val expectedArrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val actualArrivalDate: LocalDate?,
  val actualDepartureDate: LocalDate?,
  val nonArrivalConfirmedAtDateTime: LocalDateTime?,
) {
  fun isNotArrived(): Boolean = this.nonArrivalConfirmedAtDateTime != null

  fun isDepartingWithin2Weeks(nowDate: LocalDate): Boolean {
    val twoWeeksFromNow = LocalDate.now().plusWeeks(2)
    return hasArrivedAndNotDeparted() &&
      expectedDepartureDate.isAfter(nowDate) &&
      expectedDepartureDate.isBefore(twoWeeksFromNow.plusDays(1))
  }

  fun isDepartingToday(nowDate: LocalDate) = nowDate == expectedDepartureDate && hasArrivedAndNotDeparted()

  fun hasArrivedAndNotDeparted() = hasArrived() && !hasDeparted()

  private fun hasArrived() = actualArrivalDate != null

  fun hasDeparted() = actualDepartureDate != null

  fun isArrivalToday(nowDate: LocalDate): Boolean = !hasArrived() && expectedArrivalDate == nowDate

  fun isArrivalWithin2Weeks(nowDate: LocalDate): Boolean {
    val twoWeeksFromNow = LocalDate.now().plusWeeks(TWO_WEEKS)
    return !hasArrived() &&
      expectedArrivalDate.isAfter(nowDate) &&
      expectedArrivalDate.isBefore(twoWeeksFromNow.plusDays(1))
  }

  fun isArrivalWithin6Weeks(nowDate: LocalDate): Boolean {
    val sixWeeksFromNow = nowDate.plusWeeks(SIX_WEEKS)
    return !hasArrived() &&
      expectedArrivalDate.isAfter(nowDate) &&
      expectedArrivalDate.isBefore(sixWeeksFromNow.plusDays(1))
  }

  fun isOverdueArrival(nowDate: LocalDate) = !hasArrived() && nowDate.isAfter(expectedArrivalDate)

  fun isOverdueDeparture(nowDate: LocalDate) = hasArrivedAndNotDeparted() && nowDate.isAfter(expectedDepartureDate)
}
