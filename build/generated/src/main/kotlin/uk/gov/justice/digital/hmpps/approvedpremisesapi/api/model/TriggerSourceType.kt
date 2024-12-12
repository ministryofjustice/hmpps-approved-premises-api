package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: user,system
*/
enum class TriggerSourceType(val value: kotlin.String) {

    @JsonProperty("user") user("user"),
    @JsonProperty("system") system("system")
}

