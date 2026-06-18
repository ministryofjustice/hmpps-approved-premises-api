package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import java.time.LocalDate
import java.util.UUID

data class Cas1SuitableApplication(
  val id: UUID,
  val applicationStatus: ApprovedPremisesApplicationStatus,
  val requestForPlacementStatus: RequestForPlacementStatus?,
  val placementStatus: Cas1SpaceBookingStatus?,
  val premises: Cas1ExternalPremisesDto?,
)

data class Cas1ExternalPremisesDto(
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)
