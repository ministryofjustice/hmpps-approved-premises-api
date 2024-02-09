package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_WITHDRAWN_BY_PP_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables
import java.time.LocalDate

@Service
class WithdrawableService(
  // Added Lazy annotations here to prevent circular dependency issues
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  @Lazy private val placementApplicationService: PlacementApplicationService,
  @Lazy private val applicationService: ApplicationService,
) {

  fun allWithdrawables(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): Withdrawables {
    val placementRequests = placementRequestService.getWithdrawablePlacementRequests(application)
    val bookings = bookingService.getCancelleableCas1BookingsForUser(user, application)
    val placementApplications = placementApplicationService.getWithdrawablePlacementApplications(application)

    return Withdrawables(
      applicationService.isWithdrawable(application, user),
      placementRequests = placementRequests,
      bookings = bookings,
      placementApplications = placementApplications,
    )
  }

  fun withdrawAllForApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) {
    val withdrawables = allWithdrawables(
      application,
      user,
    )

    withdrawables.placementApplications.forEach {
      placementApplicationService.withdrawPlacementApplication(
        it.id,
        PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP,
      )
    }

    withdrawables.placementRequests.forEach {
      placementRequestService.withdrawPlacementRequest(
        it.id,
        user,
        PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP,
      )
    }

    val now = LocalDate.now()
    withdrawables.bookings.forEach {
      bookingService.createCancellation(
        user,
        it,
        now,
        CAS1_WITHDRAWN_BY_PP_ID,
        "Automatically withdrawn as application was withdrawn",
      )
    }
  }
}
