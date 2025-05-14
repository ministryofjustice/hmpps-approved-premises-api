package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BookingChangedContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber.EventDescriptionAndPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.time.LocalDate
import java.util.UUID

@Service
class BookingChangedTimelineFactory(val domainEventService: Cas1DomainEventService) : LegacyTimelineFactory<Cas1BookingChangedContentPayload> {

  override fun produce(domainEventId: UUID): EventDescriptionAndPayload<Cas1BookingChangedContentPayload> {
    val event = domainEventService.get(domainEventId, BookingChanged::class)!!

    return buildBookingChangedDescription(event)
  }

  private fun buildBookingChangedDescription(domainEvent: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingChanged>>) = if (domainEvent.schemaVersion == 2) {
    forSchemaVersion2(domainEvent)
  } else {
    forSchemaVersionNull(domainEvent)
  }

  private fun forSchemaVersionNull(domainEvent: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingChanged>>): EventDescriptionAndPayload<Cas1BookingChangedContentPayload> {
    val description = domainEvent.describe {
      "A placement at ${it.eventDetails.premises.name} had its arrival and/or departure date changed to " +
        "${it.eventDetails.arrivalOn.toUiFormat()} to ${it.eventDetails.departureOn.toUiFormat()}"
    }

    val eventDetails = domainEvent.data.eventDetails
    val payload = Cas1BookingChangedContentPayload(
      type = Cas1TimelineEventType.bookingChanged,
      premises = eventDetails.premises.toNamedId(),
      schemaVersion = null,
      expectedArrival = eventDetails.arrivalOn,
      expectedDeparture = eventDetails.departureOn,
    )

    return EventDescriptionAndPayload(description, payload)
  }

  private fun forSchemaVersion2(domainEvent: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingChanged>>): EventDescriptionAndPayload<Cas1BookingChangedContentPayload> {
    val eventDetails = domainEvent.data.eventDetails
    val previousArrival = eventDetails.previousArrivalOn
    val previousDeparture = eventDetails.previousDepartureOn
    val changes = mutableListOf<String>()

    fun addDateChangeMessage(previousDate: LocalDate, newDate: LocalDate, changeType: String) {
      changes.add(
        "its $changeType date changed from ${previousDate.toUiFormat()} to ${newDate.toUiFormat()}",
      )
    }

    if (previousArrival != null) {
      addDateChangeMessage(previousArrival, eventDetails.arrivalOn, "arrival")
    }

    if (previousDeparture != null) {
      addDateChangeMessage(previousDeparture, eventDetails.departureOn, "departure")
    }

    val description = if (changes.isNotEmpty()) {
      domainEvent.describe {
        "A placement at ${it.eventDetails.premises.name} had ${changes.joinToString(", ")}"
      }
    } else {
      null
    }

    return EventDescriptionAndPayload(description, getVersion2BookingChangedContentPayLoad(domainEvent))
  }

  fun getVersion2BookingChangedContentPayLoad(domainEvent: GetCas1DomainEvent<Cas1DomainEventEnvelope<BookingChanged>>): Cas1BookingChangedContentPayload? {
    val eventDetails = domainEvent.data.eventDetails
    return Cas1BookingChangedContentPayload(
      type = Cas1TimelineEventType.bookingChanged,
      premises = eventDetails.premises.toNamedId(),
      schemaVersion = domainEvent.schemaVersion,
      previousExpectedArrival = domainEvent.data.eventDetails.previousArrivalOn,
      expectedArrival = eventDetails.arrivalOn,
      previousExpectedDeparture = eventDetails.previousDepartureOn,
      expectedDeparture = eventDetails.departureOn,
      characteristics = convertToCas1SpaceCharacteristics(eventDetails.characteristics),
      previousCharacteristics = convertToCas1SpaceCharacteristics(eventDetails.previousCharacteristics),
      transferredTo = eventDetails.transferredTo?.toTimelineTransferInfo(),
    )
  }

  private fun convertToCas1SpaceCharacteristics(spaceCharacteristics: List<SpaceCharacteristic>?): List<Cas1SpaceCharacteristic>? = spaceCharacteristics?.map { spaceCharacteristic ->
    Cas1SpaceCharacteristic.valueOf(spaceCharacteristic.value)
  }

  override fun forType() = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED
}
