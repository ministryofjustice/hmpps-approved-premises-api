package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto

data class PlacementApplicationDecisionEnvelope(

  @get:JsonProperty("decision", required = true) val decision: PlacementApplicationDecisionDto,

  @get:JsonProperty("summaryOfChanges", required = true) val summaryOfChanges: String,

  @get:JsonProperty("decisionSummary", required = true) val decisionSummary: String,
)
