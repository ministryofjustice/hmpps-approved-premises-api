package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.requireXor
import java.time.OffsetDateTime
import java.util.UUID

data class Cas2SuitableApplication(
  val uiUrl: String,
  val id: UUID,
  val createdAt: OffsetDateTime,
  val createdBy: Cas2StaffDto,
  val submittedApplication: Cas2ExternalSubmittedApplicationDto?,
  val cohort: Cas2CohortDto?,
)

data class Cas2ExternalSubmittedApplicationDto(
  val latestAssessmentStatus: Cas2AssessmentStatus?,
  val offerDeclinedReason: String?,
  val cancelledReason: String?,
  val submittedAt: OffsetDateTime,
) {
  init {
    requireXor(
      latestAssessmentStatus == Cas2AssessmentStatus.CANCELLED,
      cancelledReason == null,
    ) {
      "Cancelled reason must be provided if and only if status is `cancelled`"
    }
  }

  init {
    requireXor(
      latestAssessmentStatus == Cas2AssessmentStatus.OFFER_DECLINED,
      offerDeclinedReason == null,
    ) {
      "Offer declined reason must be provided if and only if status is `offerDeclined`"
    }
  }
}
