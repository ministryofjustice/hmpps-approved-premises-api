package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3DraftReferralDeletedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3DraftReferralDeletedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Component
class Cas3DomainEventBuilder(
  @Value("\${url-templates.api.cas3.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.api.cas3.booking}") private val bookingUrlTemplate: String,
) {
  fun getBookingCancelledDomainEvent(
    booking: BookingEntity,
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

  fun getBookingConfirmedDomainEvent(
    booking: BookingEntity,
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
    booking: BookingEntity,
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

  fun getPersonArrivedDomainEvent(
    booking: BookingEntity,
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

  fun getPersonDepartedDomainEvent(
    booking: BookingEntity,
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

  fun getReferralSubmittedDomainEvent(
    application: TemporaryAccommodationApplicationEntity,
  ): DomainEvent<CAS3ReferralSubmittedEvent> {
    val domainEventId = UUID.randomUUID()

    return DomainEvent(
      id = domainEventId,
      applicationId = application.id,
      bookingId = null,
      crn = application.crn,
      nomsNumber = application.nomsNumber,
      occurredAt = application.createdAt.toInstant(),
      data = CAS3ReferralSubmittedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.referralSubmitted,
        eventDetails = CAS3ReferralSubmittedEventDetails(
          personReference = PersonReference(
            crn = application.crn,
            noms = application.nomsNumber,
          ),
          applicationId = application.id,
          applicationUrl = application.toUrl()!!,
        ),
      ),
    )
  }

  fun buildAssessmentUpdatedDomainEvent(
    assessment: TemporaryAccommodationAssessmentEntity,
    updatedFields: List<CAS3AssessmentUpdatedField>,
  ): DomainEvent<CAS3AssessmentUpdatedEvent> {
    val id = UUID.randomUUID()
    val now = Instant.now()
    return DomainEvent(
      id = id,
      crn = assessment.application.crn,
      occurredAt = now,
      data = CAS3AssessmentUpdatedEvent(
        id = id,
        updatedFields = updatedFields,
        eventType = EventType.assessmentUpdated,
        timestamp = now,
      ),
      assessmentId = assessment.id,
      nomsNumber = null,
    )
  }

  fun buildDepartureUpdatedDomainEvent(
    booking: BookingEntity,
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

  fun getBookingCancelledUpdatedDomainEvent(booking: BookingEntity, user: UserEntity?): DomainEvent<CAS3BookingCancelledUpdatedEvent> {
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
    booking: BookingEntity,
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

  fun getDraftReferralDeletedEvent(
    application: ApplicationEntity,
    user: UserEntity,
  ): DomainEvent<CAS3DraftReferralDeletedEvent> {
    val domainEventId = UUID.randomUUID()

    return DomainEvent(
      id = domainEventId,
      applicationId = application.id,
      bookingId = null,
      crn = application.crn,
      nomsNumber = null,
      occurredAt = Instant.now(),
      data = CAS3DraftReferralDeletedEvent(
        id = domainEventId,
        timestamp = Instant.now(),
        eventType = EventType.draftReferralDeleted,
        eventDetails = CAS3DraftReferralDeletedEventDetails(
          applicationId = application.id,
          crn = application.crn,
          deletedBy = user.id,
        ),
      ),
    )
  }

  private fun buildCAS3PersonDepartedEventDetail(
    booking: BookingEntity,
    application: TemporaryAccommodationApplicationEntity?,
    departure: DepartureEntity,
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
      region = booking.premises.probationRegion.name,
    ),
    departedAt = departure.dateTime.toInstant(),
    reason = departure.reason.name,
    notes = departure.notes ?: "",
    applicationId = application?.id,
    applicationUrl = application.toUrl(),
    reasonDetail = null,
    recordedBy = user?.let { populateStaffMember(it) },
  )

  private fun buildCAS3BookingCancelledEventDetails(
    application: TemporaryAccommodationApplicationEntity?,
    booking: BookingEntity,
    cancellation: CancellationEntity,
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

  private fun createCas3PersonArrivedEventDetails(
    application: TemporaryAccommodationApplicationEntity?,
    booking: BookingEntity,
    arrival: ArrivalEntity,
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
      region = booking.premises.probationRegion.name,
    ),
    arrivedAt = arrival.arrivalDateTime,
    expectedDepartureOn = arrival.expectedDepartureDate,
    notes = arrival.notes ?: "",
    recordedBy = user?.let { populateStaffMember(it) },
  )

  private fun TemporaryAccommodationApplicationEntity?.toUrl(): URI? = this?.let { URI(applicationUrlTemplate.replace("#applicationId", it.id.toString())) }

  private fun BookingEntity.toUrl(): URI = URI(bookingUrlTemplate.replace("#premisesId", this.premises.id.toString()).replace("#bookingId", this.id.toString()))

  private fun populateStaffMember(it: UserEntity) = StaffMember(
    staffCode = it.deliusStaffCode,
    username = it.deliusUsername,
    probationRegionCode = it.probationRegion.deliusCode,
  )
}
