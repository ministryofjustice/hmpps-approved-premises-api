package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: ascending,descending
*/
enum class SortOrder(val value: kotlin.String) {

    @JsonProperty("ascending") ascending("ascending"),
    @JsonProperty("descending") descending("descending")
}

