package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: past,current,future
*/
enum class Temporality(val value: kotlin.String) {

    @JsonProperty("past") past("past"),
    @JsonProperty("current") current("current"),
    @JsonProperty("future") future("future")
}

