package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: arrivingWithin6Weeks,arrivingWithin2Weeks,arrivingToday,overdueArrival,arrived,notArrived,departingWithin2Weeks,departingToday,overdueDeparture,departed
*/
enum class Cas1SpaceBookingSummaryStatus(val value: kotlin.String) {

    @JsonProperty("arrivingWithin6Weeks") arrivingWithin6Weeks("arrivingWithin6Weeks"),
    @JsonProperty("arrivingWithin2Weeks") arrivingWithin2Weeks("arrivingWithin2Weeks"),
    @JsonProperty("arrivingToday") arrivingToday("arrivingToday"),
    @JsonProperty("overdueArrival") overdueArrival("overdueArrival"),
    @JsonProperty("arrived") arrived("arrived"),
    @JsonProperty("notArrived") notArrived("notArrived"),
    @JsonProperty("departingWithin2Weeks") departingWithin2Weeks("departingWithin2Weeks"),
    @JsonProperty("departingToday") departingToday("departingToday"),
    @JsonProperty("overdueDeparture") overdueDeparture("overdueDeparture"),
    @JsonProperty("departed") departed("departed")
}

