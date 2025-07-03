package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1ChangeRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingActionsServiceTest {

  @MockK
  private lateinit var userAccessService: UserAccessService

  @MockK
  private lateinit var changeRequestRepository: Cas1ChangeRequestRepository

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingActionsService

  @BeforeEach
  fun before() {
    every { userAccessService.currentUserHasPermission(any()) } returns true
    every { changeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(any()) } returns emptyList()
  }

  @Nested
  inner class AppealCreate {

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.APPEAL_CREATE)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_PLACEMENT_APPEAL_CREATE)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.APPEAL_CREATE,
          message = "User must have permission 'CAS1_PLACEMENT_APPEAL_CREATE'",
        )
    }

    @Test
    fun `unavailable if has arrival`() {
      spaceBooking.actualArrivalDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.APPEAL_CREATE,
          message = "Space booking has been marked as arrived",
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.APPEAL_CREATE,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.APPEAL_CREATE,
          message = "Space booking has been cancelled",
        )
    }

    @Test
    fun `unavailable if already have open change request`() {
      every { changeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(any()) } returns listOf(
        Cas1ChangeRequestEntityFactory().withType(ChangeRequestType.PLACEMENT_APPEAL).produce(),
      )

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.APPEAL_CREATE,
          message = "There is an existing open change request of this type",
        )
    }
  }

  @Nested
  inner class EmergencyTransferRequest {

    var spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now())
      .withActualDepartureDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.EMERGENCY_TRANSFER_CREATE)
    }

    @Test
    fun `success if has existing cancelled transfer`() {
      val transferredBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      spaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .withActualDepartureDate(null)
        .withNonArrivalConfirmedAt(null)
        .withTransferredFrom(transferredBooking)
        .produce()

      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.EMERGENCY_TRANSFER_CREATE)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_TRANSFER_CREATE)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "User must have permission 'CAS1_TRANSFER_CREATE'",
        )
    }

    @Test
    fun `unavailable if does not have arrival`() {
      spaceBooking.actualArrivalDate = null

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "Space booking has not been marked as arrived",
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has departure`() {
      spaceBooking.actualDepartureDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "Space booking has been marked as departed",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "Space booking has been cancelled",
        )
    }

    @Test
    fun `unavailable if has a non cancelled transfer already`() {
      val transferredBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .withTransferredFrom(spaceBooking)
        .produce()

      spaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .withActualDepartureDate(null)
        .withNonArrivalConfirmedAt(null)
        .withTransferredTo(transferredBooking)
        .produce()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EMERGENCY_TRANSFER_CREATE,
          message = "Space booking has already been transferred",
        )
    }
  }

  @Nested
  inner class PlannedTransferRequest {

    var spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now())
      .withActualDepartureDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)
    }

    @Test
    fun `success if has existing cancelled transfer`() {
      val transferredBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      spaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .withActualDepartureDate(null)
        .withNonArrivalConfirmedAt(null)
        .withTransferredFrom(transferredBooking)
        .produce()

      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.PLANNED_TRANSFER_REQUEST)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_TRANSFER_CREATE)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "User must have permission 'CAS1_TRANSFER_CREATE'",
        )
    }

    @Test
    fun `unavailable if does not have arrival`() {
      spaceBooking.actualArrivalDate = null

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "Space booking has not been marked as arrived",
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has departure`() {
      spaceBooking.actualDepartureDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "Space booking has been marked as departed",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "Space booking has been cancelled",
        )
    }

    @Test
    fun `unavailable if has a non cancelled transfer already`() {
      val transferredBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .produce()

      spaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .withActualDepartureDate(null)
        .withNonArrivalConfirmedAt(null)
        .withTransferredTo(transferredBooking)
        .produce()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "Space booking has already been transferred",
        )
    }

    @Test
    fun `unavailable if already have open change request`() {
      every { changeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(any()) } returns listOf(
        Cas1ChangeRequestEntityFactory().withType(ChangeRequestType.PLANNED_TRANSFER).produce(),
      )

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.PLANNED_TRANSFER_REQUEST,
          message = "There is an existing open change request of this type",
        )
    }
  }

  @Nested
  inner class ShortenPlacementRequest {

    var spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now())
      .withActualDepartureDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.SHORTEN)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_SPACE_BOOKING_SHORTEN)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.SHORTEN,
          message = "User must have permission 'CAS1_SPACE_BOOKING_SHORTEN'",
        )
    }

    @Test
    fun `success if does not have arrival`() {
      spaceBooking.actualArrivalDate = null

      service.determineActions(spaceBooking)
        .assertAvailable(
          action = SpaceBookingAction.SHORTEN,
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.SHORTEN,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has departure`() {
      spaceBooking.actualDepartureDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.SHORTEN,
          message = "Space booking has been marked as departed",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.SHORTEN,
          message = "Space booking has been cancelled",
        )
    }

    @Test
    fun `unavailable if has a non cancelled transfer already`() {
      val transferredBooking = Cas1SpaceBookingEntityFactory()
        .withCancellationOccurredAt(null)
        .produce()

      spaceBooking = Cas1SpaceBookingEntityFactory()
        .withActualArrivalDate(LocalDate.now())
        .withActualDepartureDate(null)
        .withNonArrivalConfirmedAt(null)
        .withTransferredTo(transferredBooking)
        .produce()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.SHORTEN,
          message = "Space booking has already been transferred",
        )
    }
  }

  @Nested
  inner class ExtensionRequest {

    var spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now())
      .withActualDepartureDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.EXTENSION_REQUEST)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_EXTENSION_REQUEST)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "User must have permission 'CAS1_EXTENSION_REQUEST'",
        )
    }

    @Test
    fun `unavailable if does not have arrival`() {
      spaceBooking.actualArrivalDate = null

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "Space booking has not been marked as arrived",
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has departure`() {
      spaceBooking.actualDepartureDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "Space booking has been marked as departed",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "Space booking has been cancelled",
        )
    }

    @Test
    fun `unavailable if pending extension request`() {
      every { changeRequestRepository.findAllBySpaceBookingAndResolvedIsFalse(any()) } returns listOf(
        Cas1ChangeRequestEntityFactory().withType(ChangeRequestType.PLACEMENT_EXTENSION).produce(),
      )

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.EXTENSION_REQUEST,
          message = "There is an existing open change request of this type",
        )
    }
  }

  private fun userDoesntHavePermission(permission: UserPermission) {
    every { userAccessService.currentUserHasPermission(permission) } returns false
  }

  private fun ActionsResult.assertAvailable(action: SpaceBookingAction) {
    assertThat(this.available()).contains(action)
    assertThat(this.unavailable().map { it.action }).doesNotContain(action)
  }

  private fun ActionsResult.assertUnavailable(action: SpaceBookingAction, message: String) {
    assertThat(this.available()).doesNotContain(action)
    assertThat(this.unavailableReason(action)).isEqualTo(message)
  }
}
