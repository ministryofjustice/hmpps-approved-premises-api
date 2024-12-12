package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: assessment,placementRequest,placementApplication,bookingAppeal
*/
enum class TaskType(val value: kotlin.String) {

    @JsonProperty("Assessment") assessment("Assessment"),
    @JsonProperty("PlacementRequest") placementRequest("PlacementRequest"),
    @JsonProperty("PlacementApplication") placementApplication("PlacementApplication"),
    @JsonProperty("BookingAppeal") bookingAppeal("BookingAppeal")
}

