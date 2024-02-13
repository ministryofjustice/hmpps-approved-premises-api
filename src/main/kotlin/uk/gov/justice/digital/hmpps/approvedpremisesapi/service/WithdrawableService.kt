package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository.Constants.CAS1_WITHDRAWN_BY_PP_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.time.LocalDate

@Service
class WithdrawableService(
  // Added Lazy annotations here to prevent circular dependency issues
  @Lazy private val placementRequestService: PlacementRequestService,
  @Lazy private val bookingService: BookingService,
  @Lazy private val placementApplicationService: PlacementApplicationService,
  @Lazy private val applicationService: ApplicationService,
) {
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  fun allWithdrawables(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): Withdrawables {
    val placementRequests = placementRequestService.getWithdrawablePlacementRequestsForUser(user, application)
    val bookings = bookingService.getCancelleableCas1BookingsForUser(user, application)
    val placementApplications = placementApplicationService.getWithdrawablePlacementApplicationsForUser(user, application)

    return Withdrawables(
      applicationService.isWithdrawableForUser(user, application),
      placementRequests = placementRequests,
      bookings = bookings,
      placementApplications = placementApplications,
    )
  }

  fun withdrawAllForApplication(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ) {
    val placementRequests = application.placementRequests
    placementRequests.forEach { placementRequest ->
      if(placementRequest.isInWithdrawableState()) {
        val result = placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          user,
          PlacementRequestWithdrawalReason.WITHDRAWN_BY_PP,
          checkUserPermissions = false,
        )

        when (result) {
          is AuthorisableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw placement request ${placementRequest.id} " +
              "when withdrawing application ${application.id} " +
              "with error type ${result::class}",
          )
        }
      }
    }

    val placementApplications = placementApplicationService.getAllPlacementApplicationEntitiesForApplicationId(
      application.id
    )
    placementApplications.forEach { placementApplication ->
      if(placementApplication.isInWithdrawableState()) {
        val result = placementApplicationService.withdrawPlacementApplication(
          placementApplication.id,
          user,
          PlacementApplicationWithdrawalReason.WITHDRAWN_BY_PP,
          checkUserPermissions = false,
        )

        when (result) {
          is AuthorisableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw placement application ${placementApplication.id} " +
              "when withdrawing application ${application.id} " +
              "with error type ${result::class}",
          )
        }
      }
    }
  }
}
