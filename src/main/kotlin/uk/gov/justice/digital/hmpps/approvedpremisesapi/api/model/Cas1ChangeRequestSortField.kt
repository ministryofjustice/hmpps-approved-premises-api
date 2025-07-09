package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: NAME,TIER,CANONICAL_ARRIVAL_DATE
*/
enum class Cas1ChangeRequestSortField(@get:JsonValue val value: kotlin.String) {

    NAME("name"),
    TIER("tier"),
    CANONICAL_ARRIVAL_DATE("canonicalArrivalDate");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): Cas1ChangeRequestSortField {
                return values().first{it -> it.value == value}
        }
    }
}

