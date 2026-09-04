package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementApplicationDecisionEnvelope(

  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecisionDto,

  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: String,

  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: String,

  @Schema(description = "The acceptance details of the placement application decision. Will soon be mandatory where decision is Accepted")
  val acceptance: Cas1PlacementApplicationDecisionAcceptanceDto?,
)
