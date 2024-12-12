package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: tier,createdAt,arrivalDate,releaseType
*/
enum class ApplicationSortField(val value: kotlin.String) {

    @JsonProperty("tier") tier("tier"),
    @JsonProperty("createdAt") createdAt("createdAt"),
    @JsonProperty("arrivalDate") arrivalDate("arrivalDate"),
    @JsonProperty("releaseType") releaseType("releaseType")
}

