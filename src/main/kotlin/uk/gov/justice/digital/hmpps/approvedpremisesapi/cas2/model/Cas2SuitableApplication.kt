package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.requireXor
import java.time.Instant
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
  val latestAssessmentStatus: String?,
  val offerDeclinedReason: String?,
  val cancelledReason: String?,
  val submittedAt: OffsetDateTime,
  val markedAsArrivedDateTime: Instant?,
) {
  init {
    requireXor(
      latestAssessmentStatus == Cas2AssessmentStatus.CANCELLED.apiName,
      cancelledReason == null,
    ) {
      "Cancelled reason must be provided if and only if status is `cancelled`"
    }
  }

  init {
    requireXor(
      latestAssessmentStatus == Cas2AssessmentStatus.OFFER_DECLINED.apiName,
      offerDeclinedReason == null,
    ) {
      "Offer declined reason must be provided if and only if status is `offerDeclined`"
    }
  }

  init {
    requireXor(
      latestAssessmentStatus == Cas2AssessmentStatus.AWAITING_ARRIVAL.apiName,
      markedAsArrivedDateTime == null,
    ) {
      "Marked as arrived date time must be provided if and only if status is `awaitingArrival`"
    }
  }
}
