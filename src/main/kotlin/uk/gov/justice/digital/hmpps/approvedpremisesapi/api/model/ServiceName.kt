package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: approvedPremises,cas2,cas2v2,temporaryAccommodation
*/
enum class ServiceName(@get:JsonValue val value: kotlin.String) {

    approvedPremises("approved-premises"),
    cas2("cas2"),
    cas2v2("cas2v2"),
    temporaryAccommodation("temporary-accommodation");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): ServiceName {
                return values().first{it -> it.value == value}
        }
    }
}

