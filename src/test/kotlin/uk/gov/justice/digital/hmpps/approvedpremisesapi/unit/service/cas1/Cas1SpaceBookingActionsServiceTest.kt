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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.ActionsResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingActionsService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SpaceBookingAction
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingActionsServiceTest {

  @MockK
  private lateinit var userAccessService: Cas1UserAccessService

  @InjectMockKs
  private lateinit var service: Cas1SpaceBookingActionsService

  @BeforeEach
  fun before() {
    every { userAccessService.currentUserHasPermission(any()) } returns true
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
      every { userAccessService.currentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_SHORTEN) } returns false

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
