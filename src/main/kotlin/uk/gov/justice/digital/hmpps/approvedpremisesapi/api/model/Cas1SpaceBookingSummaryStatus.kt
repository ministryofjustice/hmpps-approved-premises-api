package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
*
* Values: arrivingWithin6Weeks,arrivingWithin2Weeks,arrivingToday,overdueArrival,arrived,notArrived,departingWithin2Weeks,departingToday,overdueDeparture,departed
*/
enum class Cas1SpaceBookingSummaryStatus(@get:JsonValue val value: kotlin.String) {

  arrivingWithin6Weeks("arrivingWithin6Weeks"),
  arrivingWithin2Weeks("arrivingWithin2Weeks"),
  arrivingToday("arrivingToday"),
  overdueArrival("overdueArrival"),
  arrived("arrived"),
  notArrived("notArrived"),
  departingWithin2Weeks("departingWithin2Weeks"),
  departingToday("departingToday"),
  overdueDeparture("overdueDeparture"),
  departed("departed"),
  ;

  companion object {
    @JvmStatic
    @JsonCreator
    fun forValue(value: kotlin.String): Cas1SpaceBookingSummaryStatus = values().first { it -> it.value == value }
  }
}
