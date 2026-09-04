package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionEnvelope

class PlacementApplicationDecisionEnvelopeFactory : Factory<PlacementApplicationDecisionEnvelope> {
  private var decision = { PlacementApplicationDecisionDto.accepted }

  fun withDecision(decision: PlacementApplicationDecisionDto) = apply {
    this.decision = { decision }
  }

  override fun produce() = PlacementApplicationDecisionEnvelope(
    decision = decision(),
    summaryOfChanges = "value",
    decisionSummary = "value",
  )
}
