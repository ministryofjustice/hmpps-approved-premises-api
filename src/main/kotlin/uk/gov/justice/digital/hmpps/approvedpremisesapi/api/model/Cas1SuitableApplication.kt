package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.util.UUID

data class Cas1SuitableApplication(
  val id: UUID,
  val applicationStatus: ApprovedPremisesApplicationStatus,
  val placementStatus: Cas1SpaceBookingStatus?,
)
