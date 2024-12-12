package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: fullPersonSummary,restrictedPersonSummary,unknownPersonSummary
*/
enum class PersonSummaryDiscriminator(val value: kotlin.String) {

    @JsonProperty("FullPersonSummary") fullPersonSummary("FullPersonSummary"),
    @JsonProperty("RestrictedPersonSummary") restrictedPersonSummary("RestrictedPersonSummary"),
    @JsonProperty("UnknownPersonSummary") unknownPersonSummary("UnknownPersonSummary")
}

