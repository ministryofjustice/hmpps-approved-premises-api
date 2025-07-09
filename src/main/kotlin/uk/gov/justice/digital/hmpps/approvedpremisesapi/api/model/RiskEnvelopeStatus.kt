package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: retrieved,notFound,error
*/
enum class RiskEnvelopeStatus(@get:JsonValue val value: kotlin.String) {

    retrieved("retrieved"),
    notFound("not_found"),
    error("error");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): RiskEnvelopeStatus {
                return values().first{it -> it.value == value}
        }
    }
}

