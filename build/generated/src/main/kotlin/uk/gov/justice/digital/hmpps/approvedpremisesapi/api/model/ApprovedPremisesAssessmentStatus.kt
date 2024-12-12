package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: awaitingResponse,completed,reallocated,inProgress,notStarted
*/
enum class ApprovedPremisesAssessmentStatus(val value: kotlin.String) {

    @JsonProperty("awaiting_response") awaitingResponse("awaiting_response"),
    @JsonProperty("completed") completed("completed"),
    @JsonProperty("reallocated") reallocated("reallocated"),
    @JsonProperty("in_progress") inProgress("in_progress"),
    @JsonProperty("not_started") notStarted("not_started")
}

