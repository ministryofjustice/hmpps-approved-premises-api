package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: standardDeterminate,life,ipp,extendedDeterminate,communityOrder,bailPlacement,nonStatutory
*/
enum class SentenceTypeOption(@get:JsonValue val value: kotlin.String) {

    standardDeterminate("standardDeterminate"),
    life("life"),
    ipp("ipp"),
    extendedDeterminate("extendedDeterminate"),
    communityOrder("communityOrder"),
    bailPlacement("bailPlacement"),
    nonStatutory("nonStatutory");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SentenceTypeOption {
                return values().first{it -> it.value == value}
        }
    }
}

