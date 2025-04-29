package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationExpiredFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingKeyWorkerAssignedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EmergencyTransferCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementAppealAcceptedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementAppealCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementAppealRejectedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

class Cas1DomainEventsFactory(val objectMapper: ObjectMapper) {

  fun createEnvelopeLatestVersion(
    type: DomainEventType,
    requestId: UUID = UUID.randomUUID(),
    occurredAt: Instant = Instant.now(),
    id: UUID = UUID.randomUUID(),
  ): DomainEventEnvelopeAndPersistedJson {
    val envelope = createEnvelopeForLatestSchemaVersion(
      id,
      type,
      requestId,
      occurredAt = occurredAt,
    )

    return DomainEventEnvelopeAndPersistedJson(
      envelope = envelope,
      persistedJson = objectMapper.writeValueAsString(envelope),
      schemaVersion = type.cas1Info!!.schemaVersions.last(),
    )
  }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun createEnvelopeForLatestSchemaVersion(
    id: UUID = UUID.randomUUID(),
    type: DomainEventType,
    requestId: UUID = UUID.randomUUID(),
    occurredAt: Instant = Instant.now(),
  ): Cas1DomainEventEnvelope<*> {
    val eventType = EventType.entries.find { it.value == type.typeName } ?: throw RuntimeException("Cannot find EventType for $type")

    val eventDetails = when (type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> ApplicationSubmittedFactory().produce()
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> ApplicationAssessedFactory().produce()
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> BookingMadeFactory().produce()
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> PersonArrivedFactory().produce()
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> PersonNotArrivedFactory().produce()
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> PersonDepartedFactory().produce()
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> BookingNotMadeFactory().produce()
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> BookingCancelledFactory().withCancellationRecordedAt(occurredAt).produce()
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> BookingChangedFactory().produce()
      DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED -> BookingKeyWorkerAssignedFactory().produce()
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> ApplicationWithdrawnFactory().produce()
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> AssessmentAppealedFactory().produce()
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> AssessmentAllocatedFactory().produce()
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawnFactory().produce()
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> PlacementApplicationAllocatedFactory().produce()
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> MatchRequestWithdrawnFactory().produce()
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> RequestForPlacementCreatedFactory().produce()
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED -> RequestForPlacementAssessedFactory().produce()
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> FurtherInformationRequestedFactory().withRequestId(requestId).produce()
      DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED -> ApplicationExpiredFactory().produce()
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_ACCEPTED -> PlacementAppealAcceptedFactory().produce()
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_CREATED -> PlacementAppealCreatedFactory().produce()
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPEAL_REJECTED -> PlacementAppealRejectedFactory().produce()
      DomainEventType.APPROVED_PREMISES_EMERGENCY_TRANSFER_CREATED -> EmergencyTransferCreatedFactory().produce()
      else -> throw RuntimeException("Domain event type $type not supported")
    }

    return Cas1DomainEventEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = eventType,
      eventDetails = eventDetails,
    )
  }

  fun removeEventDetails(json: String, fields: List<String>): String {
    val dataModel: JsonNode = objectMapper.readTree(json)
    fields.forEach {
      (dataModel["eventDetails"] as ObjectNode).remove(it)
    }
    return objectMapper.writeValueAsString(dataModel)
  }
}

data class DomainEventEnvelopeAndPersistedJson(
  val envelope: Cas1DomainEventEnvelope<*>,
  val persistedJson: String,
  val schemaVersion: DomainEventSchemaVersion,
)
