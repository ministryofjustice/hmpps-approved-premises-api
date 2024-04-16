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
      "started,STARTED",
      "submitted,SUBMITTED",
      "rejected,REJECTED",
      "awaitingAssesment,AWAITING_ASSESSMENT",
      "unallocatedAssesment,UNALLOCATED_ASSESSMENT",
      "assesmentInProgress,ASSESSMENT_IN_PROGRESS",
      "awaitingPlacement,AWAITING_PLACEMENT",
      "placementAllocated,PLACEMENT_ALLOCATED",
      "inapplicable,INAPPLICABLE",
      "withdrawn,WITHDRAWN",
      "requestedFurtherInformation,REQUESTED_FURTHER_INFORMATION",
      "pendingPlacementRequest,PENDING_PLACEMENT_REQUEST",
    ],
  )
  fun `valueOf gets the enum value from the API value`(
    apiValue: ApiApprovedPremisesApplicationStatus,
    expectedValue: ApprovedPremisesApplicationStatus,
  ) {
    assertThat(ApprovedPremisesApplicationStatus.valueOf(apiValue)).isEqualTo(expectedValue)
  }
}
