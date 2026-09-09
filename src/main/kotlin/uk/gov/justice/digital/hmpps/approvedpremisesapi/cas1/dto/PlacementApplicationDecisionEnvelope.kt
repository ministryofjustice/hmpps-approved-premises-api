package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class PlacementApplicationDecisionEnvelope(

  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecisionDto,

  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: String,

  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: String,

  @Schema(
    description = "Acceptance details for an Accepted decision. " +
      "Optional for legacy clients; required when requested duration is null.",
  )
  val acceptance: Cas1PlacementApplicationDecisionAcceptanceDto? = null,
)
