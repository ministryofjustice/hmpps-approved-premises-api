package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: accepted,rejected
*/
enum class AppealDecision(val value: kotlin.String) {

    @JsonProperty("accepted") accepted("accepted"),
    @JsonProperty("rejected") rejected("rejected")
}

