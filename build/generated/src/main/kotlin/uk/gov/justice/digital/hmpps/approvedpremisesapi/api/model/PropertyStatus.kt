package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: pending,active,archived
*/
enum class PropertyStatus(val value: kotlin.String) {

    @JsonProperty("pending") pending("pending"),
    @JsonProperty("active") active("active"),
    @JsonProperty("archived") archived("archived")
}

