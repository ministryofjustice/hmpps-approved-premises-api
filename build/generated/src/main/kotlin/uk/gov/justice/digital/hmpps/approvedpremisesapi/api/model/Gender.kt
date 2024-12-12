package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: male,female
*/
enum class Gender(val value: kotlin.String) {

    @JsonProperty("male") male("male"),
    @JsonProperty("female") female("female")
}

