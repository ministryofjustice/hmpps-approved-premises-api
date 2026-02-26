package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.util.UUID

data class Cas1SuitableApplication(
  val id: UUID,
  val applicationStatus: ApprovedPremisesApplicationStatus,
  val requestForPlacementStatus: RequestForPlacementStatus?,
  val placementStatus: Cas1SpaceBookingStatus?,
  val placementHistories: List<Cas1PlacementHistory>,
)

data class Cas1PlacementHistory(
  val dateApplied: LocalDate,
  val requestForPlacementStatus: RequestForPlacementStatus,
  val placementStatus: Cas1SpaceBookingStatus?,
)
