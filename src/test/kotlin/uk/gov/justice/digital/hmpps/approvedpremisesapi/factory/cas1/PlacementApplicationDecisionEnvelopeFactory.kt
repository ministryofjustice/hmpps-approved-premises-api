package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1PlacementApplicationDecisionAcceptanceDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionEnvelope

class PlacementApplicationDecisionEnvelopeFactory : Factory<PlacementApplicationDecisionEnvelope> {
  private var decision = { PlacementApplicationDecisionDto.accepted }
  private var acceptance: () -> Cas1PlacementApplicationDecisionAcceptanceDto? = {
    Cas1PlacementApplicationDecisionAcceptanceDto(
      Cas1AuthorisedPlacementPeriodFactory().produce(),
    )
  }

  fun withDecision(decision: PlacementApplicationDecisionDto) = apply {
    this.decision = { decision }
  }

  fun withAcceptance(acceptance: Cas1PlacementApplicationDecisionAcceptanceDto?) = apply {
    this.acceptance = { acceptance }
  }

  override fun produce() = PlacementApplicationDecisionEnvelope(
    decision = decision(),
    summaryOfChanges = "value",
    decisionSummary = "value",
    acceptance = acceptance(),
  )
}
