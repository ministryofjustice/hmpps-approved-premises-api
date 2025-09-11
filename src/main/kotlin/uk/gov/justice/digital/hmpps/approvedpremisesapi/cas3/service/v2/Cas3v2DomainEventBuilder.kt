package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class Cas3v2DomainEventBuilder(
  @Value("\${url-templates.api.cas3.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.api.cas3.booking}") private val bookingUrlTemplate: String,
) {

  fun getBookingConfirmedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity,
  ): DomainEvent<CAS3BookingConfirmedEvent> {
    val domainEventId = UUID.randomUUID()

    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = booking.createdAt.toInstant(),
      data = CAS3BookingConfirmedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetails(
          applicationId = application?.id,
          applicationUrl = application.toUrl(),
          bookingId = booking.id,
          bookingUrl = booking.toUrl(),
          personReference = PersonReference(
            crn = booking.crn,
            noms = booking.nomsNumber,
          ),
          expectedArrivedAt = booking.arrivalDate.atStartOfDay().toInstant(ZoneOffset.UTC),
          notes = "",
          confirmedBy = populateStaffMember(user),
        ),
      ),
    )
  }

  fun getBookingProvisionallyMadeDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity,
  ): DomainEvent<CAS3BookingProvisionallyMadeEvent> {
    val domainEventId = UUID.randomUUID()

    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = booking.createdAt.toInstant(),
      data = CAS3BookingProvisionallyMadeEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.bookingProvisionallyMade,
        eventDetails = CAS3BookingProvisionallyMadeEventDetails(
          applicationId = application?.id,
          applicationUrl = application.toUrl(),
          bookingId = booking.id,
          bookingUrl = booking.toUrl(),
          personReference = PersonReference(
            crn = booking.crn,
            noms = booking.nomsNumber,
          ),
          expectedArrivedAt = booking.arrivalDate.atStartOfDay().toInstant(ZoneOffset.UTC),
          notes = "",
          bookedBy = populateStaffMember(user),
        ),
      ),
    )
  }

  fun getPremisesUnarchiveEvent(
    premises: Cas3PremisesEntity,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate?,
    user: UserEntity,
  ): DomainEvent<CAS3PremisesUnarchiveEvent> {
    val domainEventId = UUID.randomUUID()

    return DomainEvent(
      id = domainEventId,
      applicationId = null,
      bookingId = null,
      crn = null,
      nomsNumber = null,
      occurredAt = Instant.now(),
      data = CAS3PremisesUnarchiveEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.premisesUnarchived,
        eventDetails = buildCAS3PremisesUnarchiveEventDetails(premises, currentStartDate, newStartDate, currentEndDate, user),
      ),
    )
  }

  private fun buildCAS3BookingCancelledEventDetails(
    application: TemporaryAccommodationApplicationEntity?,
    booking: Cas3BookingEntity,
    cancellation: Cas3CancellationEntity,
    user: UserEntity?,
  ) = CAS3BookingCancelledEventDetails(
    applicationId = application?.id,
    applicationUrl = application.toUrl(),
    bookingId = booking.id,
    bookingUrl = booking.toUrl(),
    personReference = PersonReference(
      crn = booking.crn,
      noms = booking.nomsNumber,
    ),
    cancellationReason = cancellation.reason.name,
    notes = cancellation.notes,
    cancelledAt = cancellation.date,
    cancelledBy = user?.let { populateStaffMember(it) },
  )

  fun getPersonArrivedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity?,
  ): DomainEvent<CAS3PersonArrivedEvent> {
    val domainEventId = UUID.randomUUID()

    val arrival = booking.arrival!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = arrival.arrivalDateTime,
      data = CAS3PersonArrivedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.personArrived,
        eventDetails = createCas3PersonArrivedEventDetails(application, booking, arrival, user),
      ),
    )
  }

  fun getBookingCancelledDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity,
  ): DomainEvent<CAS3BookingCancelledEvent> {
    val domainEventId = UUID.randomUUID()

    val cancellation = booking.cancellation!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = cancellation.createdAt.toInstant(),
      data = CAS3BookingCancelledEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.bookingCancelled,
        eventDetails = buildCAS3BookingCancelledEventDetails(application, booking, cancellation, user),
      ),
    )
  }

  fun getPersonDepartedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity?,
  ): DomainEvent<CAS3PersonDepartedEvent> {
    val domainEventId = UUID.randomUUID()

    val departure = booking.departure!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = departure.dateTime.toInstant(),
      data = CAS3PersonDepartedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.personDeparted,
        eventDetails = buildCAS3PersonDepartedEventDetail(booking, application, departure, user),
      ),
    )
  }

  fun buildDepartureUpdatedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity?,
  ): DomainEvent<CAS3PersonDepartureUpdatedEvent> {
    val domainEventId = UUID.randomUUID()

    val departure = booking.departure!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = departure.dateTime.toInstant(),
      data = CAS3PersonDepartureUpdatedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.personDepartureUpdated,
        eventDetails = buildCAS3PersonDepartedEventDetail(booking, application, departure, user),
      ),
    )
  }

  fun getBookingCancelledUpdatedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity?,
  ): DomainEvent<CAS3BookingCancelledUpdatedEvent> {
    val domainEventId = UUID.randomUUID()
    val cancellation = booking.cancellation!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = cancellation.createdAt.toInstant(),
      data = CAS3BookingCancelledUpdatedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.bookingCancelledUpdated,
        eventDetails = buildCAS3BookingCancelledEventDetails(application, booking, cancellation, user),
      ),
    )
  }

  fun buildPersonArrivedUpdatedDomainEvent(
    booking: Cas3BookingEntity,
    user: UserEntity?,
  ): DomainEvent<CAS3PersonArrivedUpdatedEvent> {
    val domainEventId = UUID.randomUUID()
    val arrival = booking.arrival!!
    val application = booking.application as? TemporaryAccommodationApplicationEntity

    return DomainEvent(
      id = domainEventId,
      applicationId = application?.id,
      bookingId = booking.id,
      crn = booking.crn,
      nomsNumber = booking.nomsNumber,
      occurredAt = arrival.arrivalDateTime,
      data = CAS3PersonArrivedUpdatedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.personArrivedUpdated,
        eventDetails = createCas3PersonArrivedEventDetails(application, booking, arrival, user),
      ),
    )
  }

  private fun buildCAS3PersonDepartedEventDetail(
    booking: Cas3BookingEntity,
    application: TemporaryAccommodationApplicationEntity?,
    departure: Cas3DepartureEntity,
    user: UserEntity?,
  ) = CAS3PersonDepartedEventDetails(
    personReference = PersonReference(
      crn = booking.crn,
      noms = booking.nomsNumber,
    ),
    deliusEventNumber = application?.eventNumber ?: "",
    bookingId = booking.id,
    bookingUrl = booking.toUrl(),
    premises = Premises(
      addressLine1 = booking.premises.addressLine1,
      addressLine2 = booking.premises.addressLine2,
      postcode = booking.premises.postcode,
      town = booking.premises.town,
      region = booking.premises.probationDeliveryUnit.probationRegion.name,
    ),
    departedAt = departure.dateTime.toInstant(),
    reason = departure.reason.name,
    notes = departure.notes ?: "",
    applicationId = application?.id,
    applicationUrl = application.toUrl(),
    reasonDetail = null,
    recordedBy = user?.let { populateStaffMember(it) },
  )

  private fun buildCAS3PremisesUnarchiveEventDetails(
    premises: Cas3PremisesEntity,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate?,
    user: UserEntity,
  ) = CAS3PremisesUnarchiveEventDetails(
    premisesId = premises.id,
    userId = user.id,
    currentStartDate = currentStartDate,
    newStartDate = newStartDate,
    currentEndDate = currentEndDate,
  )

  private fun populateStaffMember(it: UserEntity) = StaffMember(
    staffCode = it.deliusStaffCode,
    username = it.deliusUsername,
    probationRegionCode = it.probationRegion.deliusCode,
  )

  private fun createCas3PersonArrivedEventDetails(
    application: TemporaryAccommodationApplicationEntity?,
    booking: Cas3BookingEntity,
    arrival: Cas3ArrivalEntity,
    user: UserEntity?,
  ) = CAS3PersonArrivedEventDetails(
    applicationId = application?.id,
    applicationUrl = application.toUrl(),
    bookingId = booking.id,
    bookingUrl = booking.toUrl(),
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
      region = booking.premises.probationDeliveryUnit.probationRegion.name,
    ),
    arrivedAt = arrival.arrivalDateTime,
    expectedDepartureOn = arrival.expectedDepartureDate,
    notes = arrival.notes ?: "",
    recordedBy = user?.let { populateStaffMember(it) },
  )

  private fun Cas3BookingEntity.toUrl(): URI = URI(bookingUrlTemplate.replace("#premisesId", this.premises.id.toString()).replace("#bookingId", this.id.toString()))

  private fun TemporaryAccommodationApplicationEntity?.toUrl(): URI? = this?.let { URI(applicationUrlTemplate.replace("#applicationId", it.id.toString())) }
}
