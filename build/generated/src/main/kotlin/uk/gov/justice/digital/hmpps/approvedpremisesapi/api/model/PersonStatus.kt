package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: inCustody,inCommunity,unknown
*/
enum class PersonStatus(val value: kotlin.String) {

    @JsonProperty("InCustody") inCustody("InCustody"),
    @JsonProperty("InCommunity") inCommunity("InCommunity"),
    @JsonProperty("Unknown") unknown("Unknown")
}

