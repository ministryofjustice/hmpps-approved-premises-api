package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables

@Service
class WithdrawableService(
  // Added Lazy annotations here to prevent circular dependency issues
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  @Lazy private val placementApplicationService: PlacementApplicationService,
) {
  fun allWithdrawables(application: ApprovedPremisesApplicationEntity): Withdrawables {
    val placementRequests = placementRequestService.getWithdrawablePlacementRequests(application)
    val bookings = bookingService.getCancelleableBookings(application)
    val placementApplications = placementApplicationService.getWithdrawablePlacementApplications(application)

    return Withdrawables(
      placementRequests = placementRequests,
      bookings = bookings,
      placementApplications = placementApplications,
    )
  }
}
