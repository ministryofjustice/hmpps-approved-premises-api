package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: normal,pipe,esap,rfap,mhapStJosephs,mhapElliottHouse
*/
enum class ApType(@get:JsonValue val value: kotlin.String) {

    normal("normal"),
    pipe("pipe"),
    esap("esap"),
    rfap("rfap"),
    mhapStJosephs("mhapStJosephs"),
    mhapElliottHouse("mhapElliottHouse");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ApType {
                return values().first{it -> it.value == value}
        }
    }
}

