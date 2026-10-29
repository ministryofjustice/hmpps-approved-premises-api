package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ReleaseType
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

class PlacementRequestEntityTest {

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
      "RELATED_PLACEMENT_APPLICATION_WITHDRAWN,relatedPlacementApplicationWithdrawn",
    ],
  )
  fun `PlacementRequestWithdrawalReason#apiValue converts to the API enum values correctly`(withdrawalReason: PlacementRequestWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
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
      "RELATED_PLACEMENT_APPLICATION_WITHDRAWN,relatedPlacementApplicationWithdrawn",
    ],
  )
  fun `PlacementRequestWithdrawalReason#valueOf gets the enum value from the API value`(withdrawalReason: PlacementRequestWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
    assertThat(PlacementRequestWithdrawalReason.valueOf(apiReason)).isEqualTo(withdrawalReason)
  }

  @Nested
  inner class ResolveReleaseType {

    @Test
    fun `if placement application defined, use release type from it`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(
          PlacementApplicationEntityFactory()
            .withDefaults()
            .withReleaseType(Cas1ReleaseType.reReleasedPostRecall)
            .produce(),
        )
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withReleaseType(Cas1ReleaseType.paroleDirectedLicence)
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveReleaseType()).isEqualTo(Cas1ReleaseType.reReleasedPostRecall)
    }

    @Test
    fun `if placement application not defined, use release type from application`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(null)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withReleaseType(Cas1ReleaseType.paroleDirectedLicence)
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveReleaseType()).isEqualTo(Cas1ReleaseType.paroleDirectedLicence)
    }
  }

  @Nested
  inner class ResolveSentenceType {

    @Test
    fun `if placement application defined, use sentence type from it`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(
          PlacementApplicationEntityFactory()
            .withDefaults()
            .withSentenceType("placementAppSentenceType")
            .produce(),
        )
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withSentenceType("appSentenceType")
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveSentenceType()).isEqualTo("placementAppSentenceType")
    }

    @Test
    fun `if placement application not defined, use sentence type from application`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(null)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withSentenceType("appSentenceType")
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveSentenceType()).isEqualTo("appSentenceType")
    }
  }

  @Nested
  inner class ResolveSituation {

    @Test
    fun `if placement application defined, use situation type from it`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(
          PlacementApplicationEntityFactory()
            .withDefaults()
            .withSituation("placementAppSituation")
            .produce(),
        )
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withSituation("appSituation")
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveSituation()).isEqualTo("placementAppSituation")
    }

    @Test
    fun `if placement application not defined, use sentence type from application`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withPlacementApplication(null)
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .withSituation("appSituation")
            .produce(),
        )
        .produce()

      assertThat(placementRequest.resolveSituation()).isEqualTo("appSituation")
    }
  }

  @Nested
  inner class GetStatus {
    private val now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)

    @Test
    fun `Withdrawn with a withdrawal date uses the withdrawal date`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withCreatedAt(now.minusDays(5))
        .withIsWithdrawn(true)
        .withWithdrawalOccurredAt(now.minusDays(2))
        .produce()

      assertThat(placementRequest.getStatus()).isEqualTo(
        PlacementRequestEntity.StatusAndWhen(PlacementRequestEntity.Status.REQUEST_WITHDRAWN, now.minusDays(2).toLocalDate()),
      )
    }

    @Test
    fun `Withdrawn without a withdrawal date uses the created date`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withCreatedAt(now.minusDays(5))
        .withIsWithdrawn(true)
        .withWithdrawalOccurredAt(null)
        .produce()

      assertThat(placementRequest.getStatus()).isEqualTo(
        PlacementRequestEntity.StatusAndWhen(PlacementRequestEntity.Status.REQUEST_WITHDRAWN, now.minusDays(5).toLocalDate()),
      )
    }

    @Test
    fun `Withdrawn takes precedence over an active booking`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withIsWithdrawn(true)
        .withSpaceBookings(mutableListOf(Cas1SpaceBookingEntityFactory().withCreatedAt(now.minusDays(1)).produce()))
        .produce()

      assertThat(placementRequest.getStatus().status).isEqualTo(PlacementRequestEntity.Status.REQUEST_WITHDRAWN)
    }

    @Test
    fun `Active booking uses the oldest booking creation date`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withSpaceBookings(
          mutableListOf(
            Cas1SpaceBookingEntityFactory().withCreatedAt(now.minusDays(1)).produce(),
            Cas1SpaceBookingEntityFactory().withCreatedAt(now.minusDays(3)).produce(),
            Cas1SpaceBookingEntityFactory().withCreatedAt(now.minusDays(2)).produce(),
          ),
        )
        .produce()

      assertThat(placementRequest.getStatus()).isEqualTo(
        PlacementRequestEntity.StatusAndWhen(PlacementRequestEntity.Status.PLACEMENT_BOOKED, now.minusDays(3).toLocalDate()),
      )
    }

    @Test
    fun `Cancelled bookings are ignored`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withCreatedAt(now.minusDays(4))
        .withSpaceBookings(
          mutableListOf(
            Cas1SpaceBookingEntityFactory()
              .withCreatedAt(now.minusDays(1))
              .withCancellationOccurredAt(LocalDate.now())
              .produce(),
          ),
        )
        .produce()

      assertThat(placementRequest.getStatus()).isEqualTo(
        PlacementRequestEntity.StatusAndWhen(PlacementRequestEntity.Status.AWAITING_MATCH, now.minusDays(4).toLocalDate()),
      )
    }

    @Test
    fun `No bookings and not withdrawn is awaiting match, using the created date`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withCreatedAt(now.minusDays(6))
        .produce()

      assertThat(placementRequest.getStatus()).isEqualTo(
        PlacementRequestEntity.StatusAndWhen(PlacementRequestEntity.Status.AWAITING_MATCH, now.minusDays(6).toLocalDate()),
      )
    }
  }
}
