package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

fun createDomainEventOfType(type: DomainEventType): Any {
  val id = UUID.randomUUID()
  val timestamp = Instant.now()
  val eventType = EventType.entries.find { it.value == type.typeName } ?: throw RuntimeException("Cannot find EventType for $type")

  return when (type) {
    DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> ApplicationSubmittedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = ApplicationSubmittedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> ApplicationAssessedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = ApplicationAssessedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> BookingMadeEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = BookingMadeFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> PersonArrivedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = PersonArrivedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> PersonNotArrivedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = PersonNotArrivedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> PersonDepartedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = PersonDepartedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> BookingNotMadeEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = BookingNotMadeFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> BookingCancelledEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = BookingCancelledFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> BookingChangedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = BookingChangedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> ApplicationWithdrawnEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = ApplicationWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> AssessmentAppealedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = AssessmentAppealedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> AssessmentAllocatedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = AssessmentAllocatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawnEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = PlacementApplicationWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> PlacementApplicationAllocatedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = PlacementApplicationAllocatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> MatchRequestWithdrawnEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = MatchRequestWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> RequestForPlacementCreatedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = RequestForPlacementCreatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> FurtherInformationRequestedEnvelope(
      id = id,
      timestamp = timestamp,
      eventType = eventType,
      eventDetails = FurtherInformationRequestedFactory().produce(),
    )
    else -> throw RuntimeException("Domain event type $type not supported")
  }
}
