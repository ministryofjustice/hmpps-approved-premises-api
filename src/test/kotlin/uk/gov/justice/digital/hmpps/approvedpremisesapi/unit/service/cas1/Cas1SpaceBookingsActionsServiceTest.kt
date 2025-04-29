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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingsActionsServiceTest {

  @MockK
  private lateinit var userAccessService: UserAccessService

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingActionsService

  @BeforeEach
  fun before() {
    every { userAccessService.currentUserHasPermission(any()) } returns true
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
  }

  @Nested
  inner class TransferCreate {

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.now())
      .withActualDepartureDate(null)
      .withNonArrivalConfirmedAt(null)
      .withCancellationOccurredAt(null)
      .produce()

    @Test
    fun success() {
      service.determineActions(spaceBooking).assertAvailable(SpaceBookingAction.TRANSFER_CREATE)
    }

    @Test
    fun `unavailable if user does not have correct permission`() {
      userDoesntHavePermission(UserPermission.CAS1_TRANSFER_CREATE)

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.TRANSFER_CREATE,
          message = "User must have permission 'CAS1_TRANSFER_CREATE'",
        )
    }

    @Test
    fun `unavailable if does not have arrival`() {
      spaceBooking.actualArrivalDate = null

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.TRANSFER_CREATE,
          message = "Space booking has not been marked as arrived",
        )
    }

    @Test
    fun `unavailable if has non arrival`() {
      spaceBooking.nonArrivalConfirmedAt = Instant.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.TRANSFER_CREATE,
          message = "Space booking has been marked as non arrived",
        )
    }

    @Test
    fun `unavailable if has departure`() {
      spaceBooking.actualDepartureDate = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.TRANSFER_CREATE,
          message = "Space booking has been marked as departed",
        )
    }

    @Test
    fun `unavailable if has cancellation`() {
      spaceBooking.cancellationOccurredAt = LocalDate.now()

      service.determineActions(spaceBooking)
        .assertUnavailable(
          action = SpaceBookingAction.TRANSFER_CREATE,
          message = "Space booking has been cancelled",
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
