package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
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
}
