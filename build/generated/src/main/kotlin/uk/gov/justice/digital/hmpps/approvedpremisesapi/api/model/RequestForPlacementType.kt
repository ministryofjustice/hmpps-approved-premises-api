package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: manual,automatic
*/
enum class RequestForPlacementType(val value: kotlin.String) {

    @JsonProperty("manual") manual("manual"),
    @JsonProperty("automatic") automatic("automatic")
}

