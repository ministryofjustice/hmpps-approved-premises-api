package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: man,woman
*/
enum class Cas1ApprovedPremisesGender(val value: kotlin.String) {

    @JsonProperty("man") man("man"),
    @JsonProperty("woman") woman("woman")
}

