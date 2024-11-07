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
