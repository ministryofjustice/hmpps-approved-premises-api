package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Withdrawables
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.extractMessage
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

    val withdrawalContext = WithdrawalContext(
      user,
      WithdrawableEntityType.Application,
    )

    val placementRequests = application.placementRequests
    placementRequests.forEach { placementRequest ->
      if (placementRequest.isInWithdrawableState()) {
        val result = placementRequestService.withdrawPlacementRequest(
          placementRequest.id,
          userProvidedReason = null,
          withdrawalContext,
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
      application.id,
    )
    placementApplications.forEach { placementApplication ->
      if (placementApplication.isInWithdrawableState()) {
        val result = placementApplicationService.withdrawPlacementApplication(
          id = placementApplication.id,
          userProvidedReason = null,
          withdrawalContext,
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

    val now = LocalDate.now()
    val bookings = bookingService.getAllForApplication(application)
    bookings.forEach { booking ->
      if(booking.isInCancellableStateCas1()) {
        val bookingCancellationResult = bookingService.createCas1Cancellation(
          booking = booking,
          cancelledAt = now,
          userProvidedReason = null,
          notes = "Automatically withdrawn as placement request was withdrawn",
          withdrawalContext = withdrawalContext
        )

        when (bookingCancellationResult) {
          is ValidatableActionResult.Success -> Unit
          else -> log.error(
            "Failed to automatically withdraw booking ${booking.id} " +
              "when withdrawing application ${application.id} " +
              "with message ${extractMessage(bookingCancellationResult)}",
          )
        }
      }
    }
  }
}

data class WithdrawalContext(
  val triggeringUser: UserEntity?,
  val triggeringEntityType: WithdrawableEntityType,
)

enum class WithdrawableEntityType {
  Application,
  PlacementRequest,
  PlacementApplication,
  Booking,
}
