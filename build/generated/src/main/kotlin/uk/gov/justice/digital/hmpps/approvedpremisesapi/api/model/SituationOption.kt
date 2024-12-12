package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: riskManagement,residencyManagement,bailAssessment,bailSentence,awaitingSentence
*/
enum class SituationOption(val value: kotlin.String) {

    @JsonProperty("riskManagement") riskManagement("riskManagement"),
    @JsonProperty("residencyManagement") residencyManagement("residencyManagement"),
    @JsonProperty("bailAssessment") bailAssessment("bailAssessment"),
    @JsonProperty("bailSentence") bailSentence("bailSentence"),
    @JsonProperty("awaitingSentence") awaitingSentence("awaitingSentence")
}

