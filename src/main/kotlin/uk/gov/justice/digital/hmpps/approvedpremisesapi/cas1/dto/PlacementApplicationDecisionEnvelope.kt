package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PlacementApplicationDecisionEnvelope(

  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecisionDto,

  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: String,

  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: String,
)
