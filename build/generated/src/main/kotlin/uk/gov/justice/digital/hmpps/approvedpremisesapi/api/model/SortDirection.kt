package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: asc,desc
*/
enum class SortDirection(val value: kotlin.String) {

    @JsonProperty("asc") asc("asc"),
    @JsonProperty("desc") desc("desc")
}

