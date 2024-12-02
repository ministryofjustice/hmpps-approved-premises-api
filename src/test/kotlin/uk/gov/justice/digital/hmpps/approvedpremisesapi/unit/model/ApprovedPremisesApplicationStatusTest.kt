package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

class ApprovedPremisesApplicationStatusTest {
  @ParameterizedTest
  @CsvSource(
    value = [
      "STARTED,STARTED",
      "SUBMITTED,SUBMITTED",
      "REJECTED,REJECTED",
      "AWAITING_ASSESMENT,AWAITING_ASSESSMENT",
      "UNALLOCATED_ASSESMENT,UNALLOCATED_ASSESSMENT",
      "ASSESMENT_IN_PROGRESS,ASSESSMENT_IN_PROGRESS",
      "AWAITING_PLACEMENT,AWAITING_PLACEMENT",
      "PLACEMENT_ALLOCATED,PLACEMENT_ALLOCATED",
      "INAPPLICABLE,INAPPLICABLE",
      "WITHDRAWN,WITHDRAWN",
      "REQUESTED_FURTHER_INFORMATION,REQUESTED_FURTHER_INFORMATION",
      "PENDING_PLACEMENT_REQUEST,PENDING_PLACEMENT_REQUEST",
    ],
  )
  fun `valueOf gets the enum value from the API value`(
    apiValue: ApiApprovedPremisesApplicationStatus,
    expectedValue: ApprovedPremisesApplicationStatus,
  ) {
    assertThat(ApprovedPremisesApplicationStatus.valueOf(apiValue)).isEqualTo(expectedValue)
  }
}
