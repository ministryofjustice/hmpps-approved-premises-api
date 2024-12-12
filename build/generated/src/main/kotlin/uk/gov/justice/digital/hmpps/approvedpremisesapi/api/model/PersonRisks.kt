package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param crn 
 * @param roshRisks 
 * @param tier 
 * @param flags 
 * @param mappa 
 */
data class PersonRisks(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("crn", required = true) val crn: kotlin.String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roshRisks", required = true) val roshRisks: RoshRisksEnvelope,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("tier", required = true) val tier: RiskTierEnvelope,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("flags", required = true) val flags: FlagsEnvelope,

    @Schema(example = "null", description = "")
    @get:JsonProperty("mappa") val mappa: MappaEnvelope? = null
) {

}

