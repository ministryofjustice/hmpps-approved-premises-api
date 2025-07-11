package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus

/**
 *
 * @param status
 * @param &#x60;value&#x60;
 */
data class MappaEnvelope(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("status", required = true) val status: RiskEnvelopeStatus,

  @Schema(example = "null", description = "")
  @get:JsonProperty("value") val `value`: Mappa? = null,
)
