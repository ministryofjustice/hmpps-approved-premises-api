package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables
import java.time.LocalDate
import java.util.UUID

@Service
class WithdrawableService(
  // Added Lazy annotations here to prevent circular dependency issues
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  @Lazy private val placementApplicationService: PlacementApplicationService,
  @Lazy private val userAccessService: UserAccessService,
) {
  val approvedPremisesWithdrawnByPPBookingWithdrawnReasonId: UUID =
    UUID.fromString("d39572ea-9e42-460c-ae88-b6b30fca0b09")

  fun allWithdrawables(application: ApprovedPremisesApplicationEntity, onlyUserManageableBookings: Boolean = true): Withdrawables {
    val placementRequests = placementRequestService.getWithdrawablePlacementRequests(application)
    val bookings = bookingService.getCancelleableBookings(application)
    val placementApplications = placementApplicationService.getWithdrawablePlacementApplications(application)

    return Withdrawables(
      placementRequests = placementRequests,
      bookings = if (onlyUserManageableBookings) { bookings.filter { userAccessService.currentUserCanManagePremisesBookings(it.premises) } } else { bookings },
      placementApplications = placementApplications,
    )
  }

  fun withdrawAllForApplication(application: ApprovedPremisesApplicationEntity, user: UserEntity) {
    val withdrawables = allWithdrawables(application, false)

    withdrawables.placementApplications.forEach {
      placementApplicationService.withdrawPlacementApplication(it.id, PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP)
    }

    withdrawables.placementRequests.forEach {
      placementRequestService.withdrawPlacementRequest(it.id, user, PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP)
    }

    withdrawables.bookings.forEach {
      bookingService.createCancellation(user, it, LocalDate.now(), approvedPremisesWithdrawnByPPBookingWithdrawnReasonId, "Automatically withdrawn as application was withdrawn")
    }
  }
}
