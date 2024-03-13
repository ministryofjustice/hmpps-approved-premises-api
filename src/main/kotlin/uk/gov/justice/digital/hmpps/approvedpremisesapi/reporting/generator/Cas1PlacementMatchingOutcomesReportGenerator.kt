package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementMatchingOutcomesEntityReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.Cas1PlacementMatchingOutcomesReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.Cas1PlacementMatchingOutcomesReportProperties

class Cas1PlacementMatchingOutcomesReportGenerator :
  ReportGenerator<PlacementMatchingOutcomesEntityReportRow, Cas1PlacementMatchingOutcomesReportRow, Cas1PlacementMatchingOutcomesReportProperties>(
    Cas1PlacementMatchingOutcomesReportRow::class,
  ) {

  override fun filter(properties: Cas1PlacementMatchingOutcomesReportProperties): (PlacementMatchingOutcomesEntityReportRow) -> Boolean = {
    true
  }

  override val convert: PlacementMatchingOutcomesEntityReportRow.(properties: Cas1PlacementMatchingOutcomesReportProperties) -> List<Cas1PlacementMatchingOutcomesReportRow> = { _ ->
    listOf(
      Cas1PlacementMatchingOutcomesReportRow(
        crn = this.getCrn(),
        tier = this.getTier(),
        applicationId = this.getApplicationId(),
        requestForPlacementId = this.getRequestForPlacementId(),
        matchRequestId = this.getMatchRequestId(),
        requestForPlacementType = this.getRequestForPlacementType(),
        requestedArrivalDate = getRequestedArrivalDate()?.toLocalDate(),
        requestedDurationDays = getRequestedDurationDays(),
        requestForPlacementSubmittedAt = getRequestForPlacementSubmittedAt()?.toLocalDate(),
        requestForPlacementWithdrawalReason = getRequestForPlacementWithdrawalReason(),
        requestForPlacementAssessedDate = getRequestForPlacementAssessedDate()?.toLocalDate(),
        placementId = getPlacementId(),
        placementCancellationReason = getPlacementCancellationReason(),
      ),
    )
  }
}
