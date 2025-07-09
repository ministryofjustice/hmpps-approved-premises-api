package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: man,woman
*/
enum class Cas1ApprovedPremisesGender(@get:JsonValue val value: kotlin.String) {

    man("man"),
    woman("woman");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1ApprovedPremisesGender {
                return values().first{it -> it.value == value}
        }
    }
}

