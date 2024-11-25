package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param decision
 * @param summaryOfChanges
 * @param decisionSummary
 */
data class PlacementApplicationDecisionEnvelope(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecision,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: kotlin.String,
)
