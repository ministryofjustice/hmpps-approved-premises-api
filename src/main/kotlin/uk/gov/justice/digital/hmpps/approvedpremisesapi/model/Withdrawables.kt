package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity

data class Withdrawables(
  val application: Boolean,
  val placementRequests: List<PlacementRequestEntity>,
  val placementApplications: List<PlacementApplicationEntity>,
  val bookings: List<BookingEntity>,
)
