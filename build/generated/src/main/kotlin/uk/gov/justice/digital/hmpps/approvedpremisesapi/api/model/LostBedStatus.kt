package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: active,cancelled
*/
enum class LostBedStatus(val value: kotlin.String) {

    @JsonProperty("active") active("active"),
    @JsonProperty("cancelled") cancelled("cancelled")
}

