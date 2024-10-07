package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationExpiredEnvelope
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationExpiredFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

fun createCas1DomainEventEnvelopeForSchemaVersion(
  type: DomainEventType,
  objectMapper: ObjectMapper,
  schemaVersion: DomainEventSchemaVersion,
  requestId: UUID = UUID.randomUUID(),
  occurredAt: Instant = Instant.now(),
): DomainEventEnvelopeAndPersistedJson {
  val envelope = createCas1DomainEventEnvelopeWithLatestJson(
    type,
    requestId,
    occurredAt = occurredAt,
  )

  val persistedJson = if (type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED && schemaVersion.versionNo == null) {
    removeEventDetails(
      objectMapper,
      objectMapper.writeValueAsString(envelope),
      listOf("cancelledAtDate", "cancellationRecordedAt"),
    )
  } else {
    objectMapper.writeValueAsString(envelope)
  }

  return DomainEventEnvelopeAndPersistedJson(
    envelope = envelope,
    persistedJson = persistedJson,
  )
}

data class DomainEventEnvelopeAndPersistedJson(
  val envelope: Any,
  val persistedJson: String,
)

private fun removeEventDetails(objectMapper: ObjectMapper, json: String, fields: List<String>): String {
  val dataModel: JsonNode = objectMapper.readTree(json)
  fields.forEach {
    (dataModel["eventDetails"] as ObjectNode).remove(it)
  }
  return objectMapper.writeValueAsString(dataModel)
}

@SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
fun createCas1DomainEventEnvelopeWithLatestJson(
  type: DomainEventType,
  requestId: UUID = UUID.randomUUID(),
  occurredAt: Instant = Instant.now(),
): Any {
  val id = UUID.randomUUID()
  val eventType = EventType.entries.find { it.value == type.typeName } ?: throw RuntimeException("Cannot find EventType for $type")

  return when (type) {
    DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> ApplicationSubmittedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = ApplicationSubmittedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> ApplicationAssessedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = ApplicationAssessedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> BookingMadeEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = BookingMadeFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> PersonArrivedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = PersonArrivedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> PersonNotArrivedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = PersonNotArrivedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> PersonDepartedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = PersonDepartedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> BookingNotMadeEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = BookingNotMadeFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> {
      return BookingCancelledEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = BookingCancelledFactory()
          .withCancellationRecordedAt(occurredAt)
          .produce(),
      )
    }
    DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> BookingChangedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = BookingChangedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> ApplicationWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = ApplicationWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> AssessmentAppealedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = AssessmentAppealedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> AssessmentAllocatedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = AssessmentAllocatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = PlacementApplicationWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> PlacementApplicationAllocatedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = PlacementApplicationAllocatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> MatchRequestWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = MatchRequestWithdrawnFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> RequestForPlacementCreatedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = RequestForPlacementCreatedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED -> RequestForPlacementAssessedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = RequestForPlacementAssessedFactory().produce(),
    )
    DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> FurtherInformationRequestedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = FurtherInformationRequestedFactory()
        .withRequestId(requestId)
        .produce(),
    )
    DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED -> ApplicationExpiredEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = ApplicationExpiredFactory().produce(),
    )
    else -> throw RuntimeException("Domain event type $type not supported")
  }
}
