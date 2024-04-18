package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiPlacementApplicationDecision

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
}
