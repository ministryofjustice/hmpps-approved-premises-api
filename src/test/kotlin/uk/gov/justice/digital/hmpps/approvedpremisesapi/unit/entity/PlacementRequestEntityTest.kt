package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason

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
}
