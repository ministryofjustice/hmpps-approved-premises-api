package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.PlacementApplicationDecisionDto as ApiPlacementApplicationDecision

class PlacementApplicationEntityTest {
  @ParameterizedTest
  @CsvSource(
    value = [
      "ACCEPTED,accepted",
      "REJECTED,rejected",
      "WITHDRAW,withdraw",
      "WITHDRAWN_BY_PP,withdrawnByPp",
    ],
  )
  fun `PlacementApplicationDecision#apiValue converts to the API enum values correctly`(decision: PlacementApplicationDecision, apiDecision: ApiPlacementApplicationDecision) {
    assertThat(decision.apiValue).isEqualTo(apiDecision)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "accepted,ACCEPTED",
      "rejected,REJECTED",
      "withdraw,WITHDRAW",
      "withdrawnByPp,WITHDRAWN_BY_PP",
    ],
  )
  fun `PlacementApplicationDecision#valueOf gets the enum value from the API value`(apiDecision: ApiPlacementApplicationDecision, decision: PlacementApplicationDecision) {
    assertThat(PlacementApplicationDecision.valueOf(apiDecision)).isEqualTo(decision)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "DUPLICATE_PLACEMENT_REQUEST,duplicatePlacementRequest",
      "ALTERNATIVE_PROVISION_IDENTIFIED,alternativeProvisionIdentified",
      "WITHDRAWN_BY_PP,withdrawnByPP",
      "CHANGE_IN_CIRCUMSTANCES,changeInCircumstances",
      "CHANGE_IN_RELEASE_DECISION,changeInReleaseDecision",
      "NO_CAPACITY_DUE_TO_LOST_BED,noCapacityDueToLostBed",
      "NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,noCapacityDueToPlacementPrioritisation",
      "NO_CAPACITY,noCapacity",
      "ERROR_IN_PLACEMENT_REQUEST,errorInPlacementRequest",
      "RELATED_APPLICATION_WITHDRAWN,relatedApplicationWithdrawn",
    ],
  )
  fun `PlacementApplicationWithdrawalReason#apiValue converts to the API enum values correctly`(withdrawalReason: PlacementApplicationWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
    assertThat(withdrawalReason.apiValue).isEqualTo(apiReason)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "DUPLICATE_PLACEMENT_REQUEST,duplicatePlacementRequest",
      "ALTERNATIVE_PROVISION_IDENTIFIED,alternativeProvisionIdentified",
      "WITHDRAWN_BY_PP,withdrawnByPP",
      "CHANGE_IN_CIRCUMSTANCES,changeInCircumstances",
      "CHANGE_IN_RELEASE_DECISION,changeInReleaseDecision",
      "NO_CAPACITY_DUE_TO_LOST_BED,noCapacityDueToLostBed",
      "NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,noCapacityDueToPlacementPrioritisation",
      "NO_CAPACITY,noCapacity",
      "ERROR_IN_PLACEMENT_REQUEST,errorInPlacementRequest",
      "RELATED_APPLICATION_WITHDRAWN,relatedApplicationWithdrawn",
    ],
  )
  fun `PlacementApplicationWithdrawalReason#valueOf gets the enum value from the API value`(withdrawalReason: PlacementApplicationWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
    assertThat(PlacementApplicationWithdrawalReason.valueOf(apiReason)).isEqualTo(withdrawalReason)
  }

  @Nested
  inner class DeriveStatus {
    private val now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    @Test
    fun `Withdrawn with a withdrawal date uses the withdrawal date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withSubmittedAt(now.minusDays(5))
        .withIsWithdrawn(true)
        .withWithdrawalOccurredAt(now.minusDays(2))
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.REQUEST_WITHDRAWN, now.minusDays(2).toLocalDate()),
      )
    }

    @Test
    fun `Withdrawn without a withdrawal date uses the submitted date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withSubmittedAt(now.minusDays(5))
        .withIsWithdrawn(true)
        .withWithdrawalOccurredAt(null)
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.REQUEST_WITHDRAWN, now.minusDays(5).toLocalDate()),
      )
    }

    @Test
    fun `Withdrawn takes precedence over an active booking`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withIsWithdrawn(true)
        .withWithdrawalOccurredAt(now)
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withDecisionMadeAt(now)
        .withPlacementRequest(placementRequestWithBookings(now.minusDays(1)))
        .produce()

      assertThat(placementApplication.getStatus().status).isEqualTo(PlacementApplicationEntity.Status.REQUEST_WITHDRAWN)
    }

    @Test
    fun `Active booking uses the oldest booking creation date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withDecisionMadeAt(now)
        .withPlacementRequest(placementRequestWithBookings(now.minusDays(1), now.minusDays(3), now.minusDays(2)))
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.PLACEMENT_BOOKED, now.minusDays(3).toLocalDate()),
      )
    }

    @Test
    fun `Cancelled bookings are ignored`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withDecisionMadeAt(now)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .produce()
            .apply {
              spaceBookings = mutableListOf(
                Cas1SpaceBookingEntityFactory()
                  .withCreatedAt(now.minusDays(1))
                  .withCancellationOccurredAt(LocalDate.now())
                  .produce(),
              )
            },
        )
        .produce()

      assertThat(placementApplication.getStatus().status).isEqualTo(PlacementApplicationEntity.Status.AWAITING_MATCH)
    }

    @Test
    fun `Rejected uses the decision date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withDecision(PlacementApplicationDecision.REJECTED)
        .withDecisionMadeAt(now.minusDays(1))
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.REQUEST_REJECTED, now.minusDays(1).toLocalDate()),
      )
    }

    @Test
    fun `Accepted without a booking uses the decision date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withDecision(PlacementApplicationDecision.ACCEPTED)
        .withDecisionMadeAt(now.minusDays(1))
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.AWAITING_MATCH, now.minusDays(1).toLocalDate()),
      )
    }

    @Test
    fun `Submitted without a decision uses the submitted date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withSubmittedDefaults()
        .withSubmittedAt(now.minusDays(4))
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.REQUEST_SUBMITTED, now.minusDays(4).toLocalDate()),
      )
    }

    @Test
    fun `Unsubmitted uses the created date`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .withCreatedAt(now.minusDays(6))
        .withSubmittedAt(null)
        .produce()

      assertThat(placementApplication.getStatus()).isEqualTo(
        PlacementApplicationEntity.StatusAndWhen(PlacementApplicationEntity.Status.REQUEST_UNSUBMITTED, now.minusDays(6).toLocalDate()),
      )
    }

    private fun placementRequestWithBookings(vararg createdAt: OffsetDateTime) = PlacementRequestEntityFactory()
      .withDefaults()
      .produce()
      .apply {
        spaceBookings = createdAt.map { Cas1SpaceBookingEntityFactory().withCreatedAt(it).produce() }.toMutableList()
      }
  }
}
