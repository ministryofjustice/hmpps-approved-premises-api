package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: retrieved,notFound,error
*/
enum class RiskEnvelopeStatus(val value: kotlin.String) {

    @JsonProperty("retrieved") retrieved("retrieved"),
    @JsonProperty("not_found") notFound("not_found"),
    @JsonProperty("error") error("error")
}

