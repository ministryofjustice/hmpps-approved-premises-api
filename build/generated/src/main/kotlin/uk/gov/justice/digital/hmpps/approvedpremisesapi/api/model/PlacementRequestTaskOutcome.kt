package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: matched,unableToMatch
*/
enum class PlacementRequestTaskOutcome(val value: kotlin.String) {

    @JsonProperty("matched") matched("matched"),
    @JsonProperty("unable_to_match") unableToMatch("unable_to_match")
}

