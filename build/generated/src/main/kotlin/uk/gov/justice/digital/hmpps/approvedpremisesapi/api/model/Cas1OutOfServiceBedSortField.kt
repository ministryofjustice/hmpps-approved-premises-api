package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: premisesName,roomName,bedName,startDate,endDate,reason,daysLost
*/
enum class Cas1OutOfServiceBedSortField(val value: kotlin.String) {

    @JsonProperty("premisesName") premisesName("premisesName"),
    @JsonProperty("roomName") roomName("roomName"),
    @JsonProperty("bedName") bedName("bedName"),
    @JsonProperty("startDate") startDate("startDate"),
    @JsonProperty("endDate") endDate("endDate"),
    @JsonProperty("reason") reason("reason"),
    @JsonProperty("daysLost") daysLost("daysLost")
}

