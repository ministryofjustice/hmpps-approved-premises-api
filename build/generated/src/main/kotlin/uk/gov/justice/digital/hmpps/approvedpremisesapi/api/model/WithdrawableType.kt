package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: application,booking,placementApplication,placementRequest,spaceBooking
*/
enum class WithdrawableType(val value: kotlin.String) {

    @JsonProperty("application") application("application"),
    @JsonProperty("booking") booking("booking"),
    @JsonProperty("placement_application") placementApplication("placement_application"),
    @JsonProperty("placement_request") placementRequest("placement_request"),
    @JsonProperty("space_booking") spaceBooking("space_booking")
}

