package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.time.Instant
import java.util.UUID

@Component
class DomainEventBuilder {
  fun getPersonArrivedDomainEvent(
    booking: BookingEntity,
  ): DomainEvent<CAS3PersonArrivedEvent> {
    val domainEventId = UUID.randomUUID()

    val arrival = booking.arrival!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id ?: UUID(0L, 0L),
      crn = booking.crn,
      occurredAt = arrival.arrivalDateTime,
      data = CAS3PersonArrivedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetails(
          applicationId = application?.id,
          bookingId = booking.id,
          personReference = PersonReference(
            crn = booking.crn,
            noms = booking.nomsNumber,
          ),
          deliusEventNumber = application?.eventNumber ?: "",
          premises = Premises(
            addressLine1 = booking.premises.addressLine1,
            addressLine2 = booking.premises.addressLine2,
            postcode = booking.premises.postcode,
            town = booking.premises.town,
            region = booking.premises.probationRegion.name,
          ),
          arrivedAt = arrival.arrivalDateTime,
          expectedDepartureOn = arrival.expectedDepartureDate,
          notes = arrival.notes ?: "",
        ),
      ),
    )
  }
}
