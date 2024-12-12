package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: completed,incomplete
*/
enum class OASysAssessmentState(val value: kotlin.String) {

    @JsonProperty("Completed") completed("Completed"),
    @JsonProperty("Incomplete") incomplete("Incomplete")
}

