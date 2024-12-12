package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: notMatched,unableToMatch,matched
*/
enum class PlacementRequestStatus(val value: kotlin.String) {

    @JsonProperty("notMatched") notMatched("notMatched"),
    @JsonProperty("unableToMatch") unableToMatch("unableToMatch"),
    @JsonProperty("matched") matched("matched")
}

