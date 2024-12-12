package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: fullPerson,restrictedPerson,unknownPerson
*/
enum class PersonType(val value: kotlin.String) {

    @JsonProperty("FullPerson") fullPerson("FullPerson"),
    @JsonProperty("RestrictedPerson") restrictedPerson("RestrictedPerson"),
    @JsonProperty("UnknownPerson") unknownPerson("UnknownPerson")
}

