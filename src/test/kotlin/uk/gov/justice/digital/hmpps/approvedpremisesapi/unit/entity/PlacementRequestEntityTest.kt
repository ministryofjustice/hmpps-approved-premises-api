package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestTaskOutcome
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingNotMadeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime

class PlacementRequestEntityTest {
  @Nested
  inner class GetOutcomeDetails {
    @Test
    fun `When a booking not made exists, then returns its creation time as the outcome timestamp and 'unableToMatch' as the outcome`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val expectedOutcomeTimestamp = OffsetDateTime.now().randomDateTimeBefore(14)

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .withCreatedAt(expectedOutcomeTimestamp.randomDateTimeBefore(14))
        .produce()
        .apply {
          this.bookingNotMades = mutableListOf(
            BookingNotMadeEntityFactory()
              .withPlacementRequest(this)
              .withCreatedAt(expectedOutcomeTimestamp)
              .produce(),
          )
        }

      val (actualOutcomeTimestamp, outcome) = placementRequest.getOutcomeDetails()

      assertThat(actualOutcomeTimestamp).isEqualTo(expectedOutcomeTimestamp)
      assertThat(outcome).isEqualTo(PlacementRequestTaskOutcome.unableToMatch)
    }

    @Test
    fun `When an active booking exists, then returns its creation time as the outcome timestamp and 'matched' as the outcome`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val expectedOutcomeTimestamp = OffsetDateTime.now().randomDateTimeBefore(14)

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .withCreatedAt(expectedOutcomeTimestamp.randomDateTimeBefore(14))
        .withBooking {
          withApplication(application)
          withCreatedAt(expectedOutcomeTimestamp)
          withDefaultPremises()
        }
        .produce()

      val (actualOutcomeTimestamp, outcome) = placementRequest.getOutcomeDetails()

      assertThat(actualOutcomeTimestamp).isEqualTo(expectedOutcomeTimestamp)
      assertThat(outcome).isEqualTo(PlacementRequestTaskOutcome.matched)
    }

    @Test
    fun `Returns no data otherwise`() {
      val user = UserEntityFactory()
        .withDefaults()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      val placementRequirements = PlacementRequirementsEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withAssessment(assessment)
        .withPlacementRequirements(placementRequirements)
        .withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
        .produce()

      val (actualOutcomeTimestamp, outcome) = placementRequest.getOutcomeDetails()

      assertThat(actualOutcomeTimestamp).isNull()
      assertThat(outcome).isNull()
    }
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
}
