package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: CAS1,CAS2,CAS3,CAS2V2
*/
enum class UpdateApplicationType(@get:JsonValue val value: kotlin.String) {

    CAS1("CAS1"),
    CAS2("CAS2"),
    CAS3("CAS3"),
    CAS2V2("CAS2V2");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): UpdateApplicationType {
                return values().first{it -> it.value == value}
        }
    }
}

