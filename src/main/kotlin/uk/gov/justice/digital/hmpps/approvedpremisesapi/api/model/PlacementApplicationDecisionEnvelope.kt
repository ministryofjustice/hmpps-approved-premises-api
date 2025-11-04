package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PlacementApplicationDecisionEnvelope(

  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecision,

  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: kotlin.String,

  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: kotlin.String,
)
