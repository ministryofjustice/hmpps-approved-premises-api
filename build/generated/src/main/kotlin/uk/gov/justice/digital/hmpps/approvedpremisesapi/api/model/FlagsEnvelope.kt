package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param status 
 * @param &#x60;value&#x60; 
 */
data class FlagsEnvelope(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

    @Schema(example = "null", description = "")
    @get:JsonProperty("value") val `value`: kotlin.collections.List<kotlin.String>? = null
) {

}

