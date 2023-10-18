package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.OffsetDateTime

data class TimelineEvent(
  val id: String,
  val type: TimelineEventType,
  val occurredAt: OffsetDateTime,
)

enum class TimelineEventType {
  APPROVED_PREMISES_APPLICATION_SUBMITTED,
  APPROVED_PREMISES_APPLICATION_ASSESSED,
  APPROVED_PREMISES_BOOKING_MADE,
  APPROVED_PREMISES_PERSON_ARRIVED,
  APPROVED_PREMISES_PERSON_NOT_ARRIVED,
  APPROVED_PREMISES_PERSON_DEPARTED,
  APPROVED_PREMISES_BOOKING_NOT_MADE,
  APPROVED_PREMISES_BOOKING_CANCELLED,
  APPROVED_PREMISES_BOOKING_CHANGED,
  APPROVED_PREMISES_APPLICATION_WITHDRAWN,
  APPROVED_PREMISES_INFORMATION_REQUEST,
  CAS3_PERSON_ARRIVED,
  CAS3_PERSON_DEPARTED,
}
