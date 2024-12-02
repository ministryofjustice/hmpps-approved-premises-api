package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.SpaceBookingDates
import java.time.LocalDate

@Component
class Cas1SpaceBookingStatusTransformer {

  fun transformToSpaceBookingSummaryStatus(
    spaceBookingDates: SpaceBookingDates,
  ): Cas1SpaceBookingSummaryStatus? {
    val nowDate = LocalDate.now()
    return when {
      spaceBookingDates.isNotArrived() -> Cas1SpaceBookingSummaryStatus.NOT_ARRIVED
      spaceBookingDates.hasDeparted() -> Cas1SpaceBookingSummaryStatus.DEPARTED
      spaceBookingDates.isOverdueDeparture(nowDate) -> Cas1SpaceBookingSummaryStatus.OVERDUE_DEPARTURE
      spaceBookingDates.isDepartingToday(nowDate) -> Cas1SpaceBookingSummaryStatus.DEPARTING_TODAY
      spaceBookingDates.isDepartingWithin2Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.DEPARTING_WITHIN2_WEEKS
      spaceBookingDates.hasArrivedAndNotDeparted() -> Cas1SpaceBookingSummaryStatus.ARRIVED
      spaceBookingDates.isArrivalToday(nowDate) -> Cas1SpaceBookingSummaryStatus.ARRIVING_TODAY
      spaceBookingDates.isArrivalWithin2Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.ARRIVING_WITHIN2_WEEKS
      spaceBookingDates.isArrivalWithin6Weeks(nowDate) -> Cas1SpaceBookingSummaryStatus.ARRIVING_WITHIN6_WEEKS
      spaceBookingDates.isOverdueArrival(nowDate) -> Cas1SpaceBookingSummaryStatus.OVERDUE_ARRIVAL
      else -> null
    }
  }
}
