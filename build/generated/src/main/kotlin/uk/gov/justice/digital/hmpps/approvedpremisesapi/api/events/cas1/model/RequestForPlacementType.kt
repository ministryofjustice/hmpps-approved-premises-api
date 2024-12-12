package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: initial,rotl,releaseFollowingDecisions,additionalPlacement
*/
enum class RequestForPlacementType(val value: kotlin.String) {

    @JsonProperty("initial") initial("initial"),
    @JsonProperty("rotl") rotl("rotl"),
    @JsonProperty("releaseFollowingDecisions") releaseFollowingDecisions("releaseFollowingDecisions"),
    @JsonProperty("additionalPlacement") additionalPlacement("additionalPlacement")
}

