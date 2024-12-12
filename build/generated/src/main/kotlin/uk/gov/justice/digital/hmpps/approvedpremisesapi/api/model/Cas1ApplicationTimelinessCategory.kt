package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: standard,emergency,shortNotice
*/
enum class Cas1ApplicationTimelinessCategory(val value: kotlin.String) {

    @JsonProperty("standard") standard("standard"),
    @JsonProperty("emergency") emergency("emergency"),
    @JsonProperty("shortNotice") shortNotice("shortNotice")
}

