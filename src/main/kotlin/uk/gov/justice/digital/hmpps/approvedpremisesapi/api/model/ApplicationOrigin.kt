package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: courtBail,prisonBail,homeDetentionCurfew
*/
enum class ApplicationOrigin(@get:JsonValue val value: kotlin.String) {

    courtBail("courtBail"),
    prisonBail("prisonBail"),
    homeDetentionCurfew("homeDetentionCurfew");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApplicationOrigin {
                return values().first{it -> it.value == value}
        }
    }
}

