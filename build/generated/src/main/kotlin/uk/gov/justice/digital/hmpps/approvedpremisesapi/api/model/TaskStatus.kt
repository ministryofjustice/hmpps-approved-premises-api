package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: notStarted,inProgress,complete,infoRequested
*/
enum class TaskStatus(val value: kotlin.String) {

    @JsonProperty("not_started") notStarted("not_started"),
    @JsonProperty("in_progress") inProgress("in_progress"),
    @JsonProperty("complete") complete("complete"),
    @JsonProperty("info_requested") infoRequested("info_requested")
}

