package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: rotl,releaseFollowingDecision,additionalPlacement
*/
enum class PlacementType(val value: kotlin.String) {

    @JsonProperty("rotl") rotl("rotl"),
    @JsonProperty("release_following_decision") releaseFollowingDecision("release_following_decision"),
    @JsonProperty("additional_placement") additionalPlacement("additional_placement")
}

