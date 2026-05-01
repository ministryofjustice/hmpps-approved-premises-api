package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class PlacementApplicationDecisionEnvelope(

  val decision: PlacementApplicationDecision,

  val summaryOfChanges: kotlin.String,

  val decisionSummary: kotlin.String,
)
