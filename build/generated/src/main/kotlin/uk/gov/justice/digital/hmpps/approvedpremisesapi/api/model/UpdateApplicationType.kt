package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: CAS1,CAS2,CAS3
*/
enum class UpdateApplicationType(val value: kotlin.String) {

    @JsonProperty("CAS1") CAS1("CAS1"),
    @JsonProperty("CAS2") CAS2("CAS2"),
    @JsonProperty("CAS3") CAS3("CAS3")
}

