package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: riskManagement,residencyManagement,bailAssessment,bailSentence,awaitingSentence
*/
enum class SituationOption(@get:JsonValue val value: kotlin.String) {

    riskManagement("riskManagement"),
    residencyManagement("residencyManagement"),
    bailAssessment("bailAssessment"),
    bailSentence("bailSentence"),
    awaitingSentence("awaitingSentence");

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: kotlin.String): SituationOption {
                return values().first{it -> it.value == value}
        }
    }
}

