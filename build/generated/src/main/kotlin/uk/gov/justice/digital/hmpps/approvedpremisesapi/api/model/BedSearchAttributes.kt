package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: SHARED_PROPERTY,SINGLE_OCCUPANCY,WHEELCHAIR_ACCESSIBLE
*/
enum class BedSearchAttributes(val value: kotlin.String) {

    @JsonProperty("isSharedProperty") SHARED_PROPERTY("isSharedProperty"),
    @JsonProperty("isSingleOccupancy") SINGLE_OCCUPANCY("isSingleOccupancy"),
    @JsonProperty("isWheelchairAccessible") WHEELCHAIR_ACCESSIBLE("isWheelchairAccessible")
}

