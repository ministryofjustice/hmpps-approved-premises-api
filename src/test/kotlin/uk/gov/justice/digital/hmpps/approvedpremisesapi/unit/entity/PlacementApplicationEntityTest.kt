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
      "ACCEPTED,ACCEPTED",
      "REJECTED,REJECTED",
      "WITHDRAW,WITHDRAW",
      "WITHDRAWN_BY_PP,WITHDRAWN_BY_PP",
    ],
  )
  fun `PlacementApplicationDecision#apiValue converts to the API enum values correctly`(decision: PlacementApplicationDecision, apiDecision: ApiPlacementApplicationDecision) {
    assertThat(decision.apiValue).isEqualTo(apiDecision)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "ACCEPTED,ACCEPTED",
      "REJECTED,REJECTED",
      "WITHDRAW,WITHDRAW",
      "WITHDRAWN_BY_PP,WITHDRAWN_BY_PP",
    ],
  )
  fun `PlacementApplicationDecision#valueOf gets the enum value from the API value`(apiDecision: ApiPlacementApplicationDecision, decision: PlacementApplicationDecision) {
    assertThat(PlacementApplicationDecision.valueOf(apiDecision)).isEqualTo(decision)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "DUPLICATE_PLACEMENT_REQUEST,DUPLICATE_PLACEMENT_REQUEST",
      "ALTERNATIVE_PROVISION_IDENTIFIED,ALTERNATIVE_PROVISION_IDENTIFIED",
      "WITHDRAWN_BY_PP,WITHDRAWN_BY_PP",
      "CHANGE_IN_CIRCUMSTANCES,CHANGE_IN_CIRCUMSTANCES",
      "CHANGE_IN_RELEASE_DECISION,CHANGE_IN_RELEASE_DECISION",
      "NO_CAPACITY_DUE_TO_LOST_BED,NO_CAPACITY_DUE_TO_LOST_BED",
      "NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION",
      "NO_CAPACITY,NO_CAPACITY",
      "ERROR_IN_PLACEMENT_REQUEST,ERROR_IN_PLACEMENT_REQUEST",
      "RELATED_APPLICATION_WITHDRAWN,RELATED_APPLICATION_WITHDRAWN",
    ],
  )
  fun `PlacementApplicationWithdrawalReason#apiValue converts to the API enum values correctly`(withdrawalReason: PlacementApplicationWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
    assertThat(withdrawalReason.apiValue).isEqualTo(apiReason)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "DUPLICATE_PLACEMENT_REQUEST,DUPLICATE_PLACEMENT_REQUEST",
      "ALTERNATIVE_PROVISION_IDENTIFIED,ALTERNATIVE_PROVISION_IDENTIFIED",
      "WITHDRAWN_BY_PP,WITHDRAWN_BY_PP",
      "CHANGE_IN_CIRCUMSTANCES,CHANGE_IN_CIRCUMSTANCES",
      "CHANGE_IN_RELEASE_DECISION,CHANGE_IN_RELEASE_DECISION",
      "NO_CAPACITY_DUE_TO_LOST_BED,NO_CAPACITY_DUE_TO_LOST_BED",
      "NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION",
      "NO_CAPACITY,NO_CAPACITY",
      "ERROR_IN_PLACEMENT_REQUEST,ERROR_IN_PLACEMENT_REQUEST",
      "RELATED_APPLICATION_WITHDRAWN,RELATED_APPLICATION_WITHDRAWN",
    ],
  )
  fun `PlacementApplicationWithdrawalReason#valueOf gets the enum value from the API value`(withdrawalReason: PlacementApplicationWithdrawalReason, apiReason: WithdrawPlacementRequestReason) {
    assertThat(PlacementApplicationWithdrawalReason.valueOf(apiReason)).isEqualTo(withdrawalReason)
  }
}
