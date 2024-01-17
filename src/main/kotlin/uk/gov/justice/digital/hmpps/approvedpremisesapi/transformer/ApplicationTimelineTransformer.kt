package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary

@Component
class ApplicationTimelineTransformer {

  fun transformDomainEventSummaryToTimelineEvent(domainEventSummary: DomainEventSummary): TimelineEvent {
    return TimelineEvent(
      id = domainEventSummary.id,
      type = transformDomainEventTypeToTimelineEventType(domainEventSummary.type),
      occurredAt = domainEventSummary.occurredAt.toInstant(),
    )
  }

  fun transformDomainEventTypeToTimelineEventType(domainEventType: DomainEventType): TimelineEventType {
    return when (domainEventType) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> TimelineEventType.approvedPremisesApplicationSubmitted
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> TimelineEventType.approvedPremisesApplicationAssessed
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> TimelineEventType.approvedPremisesBookingMade
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> TimelineEventType.approvedPremisesPersonArrived
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> TimelineEventType.approvedPremisesPersonNotArrived
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> TimelineEventType.approvedPremisesPersonDeparted
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> TimelineEventType.approvedPremisesBookingNotMade
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> TimelineEventType.approvedPremisesBookingCancelled
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> TimelineEventType.approvedPremisesBookingChanged
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> TimelineEventType.approvedPremisesApplicationWithdrawn
      else -> throw IllegalArgumentException("Only CAS1 is currently supported")
    }
  }
}
