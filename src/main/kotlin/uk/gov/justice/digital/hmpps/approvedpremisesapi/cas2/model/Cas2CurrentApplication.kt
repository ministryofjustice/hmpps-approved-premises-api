package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.requireXor
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class Cas2CurrentApplication(
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
  @Deprecated("Use markedAsArrived instead") val markedAsArrivedDateTime: Instant?,
  val markedAsArrived: Cas2MarkedAsArrived?,
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
    require(
      latestAssessmentStatus == Cas2AssessmentStatus.AWAITING_ARRIVAL.apiName || markedAsArrived == null,
    ) {
      "Marked as arrived can only be provided when status is `awaitingArrival`"
    }
  }
}

data class Cas2MarkedAsArrived(
  val dateTime: Instant,
  val arrivedByUsername: String,
)
