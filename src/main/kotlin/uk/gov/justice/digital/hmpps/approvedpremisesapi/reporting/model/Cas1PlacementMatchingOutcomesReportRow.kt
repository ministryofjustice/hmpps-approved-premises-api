package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class Cas1PlacementMatchingOutcomesReportRow(
  val crn: String?,
  val applicationId: String?,
  val requestForPlacementId: String?,
  val matchRequestId: String?,
  val requestForPlacementType: String?,
  val requestedArrivalDate: LocalDate?,
  val requestedDurationDays: Int?,
  val requestForPlacementSubmittedAt: LocalDate?,
  val requestForPlacementWithdrawalReason: String?,
  val requestForPlacementAssessedDate: LocalDate?,
  val placementId: String?,
  val placementCancellationReason: String?,
)
