package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SuppressWarnings("CyclomaticComplexMethod")
class Cas3DomainEventBuilderTest {
  private val cas3DomainEventBuilder =
    Cas3DomainEventBuilder(
      applicationUrlTemplate = "http://api/applications/#applicationId",
      bookingUrlTemplate = "http://api/premises/#premisesId/bookings/#bookingId",
    )

  @Test
  fun `buildAssessmentUpdatedDomainEvent builds correct domain event`() {
    val id = UUID.randomUUID()
    val application = TemporaryAccommodationApplicationEntityFactory().withDefaults().withCrn("A123456").produce()
    val assessment = TemporaryAccommodationAssessmentEntityFactory().withApplication(application).withId(id).produce()
    val updatedField = CAS3AssessmentUpdatedField("TESTFIELD", "A", "B")
    val event = cas3DomainEventBuilder.buildAssessmentUpdatedDomainEvent(assessment, listOf(updatedField))

    assertThat(event.applicationId).isNull()
    assertThat(event.assessmentId).isEqualTo(assessment.id)
    assertThat(event.bookingId).isNull()
    assertThat(event.cas1SpaceBookingId).isNull()
    assertThat(event.crn).isEqualTo("A123456")
    assertThat(event.nomsNumber).isNull()
    assertThat(event.occurredAt).isWithinTheLastMinute()
    assertThat(event.metadata).isEmpty()
    assertThat(event.schemaVersion).isNull()

    val data = event.data

    assertThat(data::class).isEqualTo(CAS3AssessmentUpdatedEvent::class)
    assertThat(data.updatedFields.size).isEqualTo(1)
    assertThat(data.updatedFields.get(0)).isEqualTo(updatedField)

    assertThat(data.eventType).isEqualTo(EventType.assessmentUpdated)
  }

  @Test
  fun `getBookingCancelledDomainEvent transforms the booking information correctly`() {
    val cancellationReasonName = "Some cancellation reason"
    val cancellationNotes = "Some notes about the cancellation"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    val cancellationReason = cancellationReasonEntity(cancellationReasonName)

    booking.cancellations += cancellationEntity(booking, cancellationReason, cancellationNotes)

    val event = cas3DomainEventBuilder.getBookingCancelledDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.cancellationReason == cancellationReasonName &&
        data.notes == cancellationNotes &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}"
      data.cancelledAt == booking.cancellation?.date &&
        data.cancelledBy!!.staffCode == user.deliusStaffCode &&
        data.cancelledBy!!.username == user.deliusUsername &&
        data.cancelledBy!!.probationRegionCode == user.probationRegion.deliusCode &&
        it.data.eventType == EventType.bookingCancelled
    }
  }

  @Test
  fun `getBookingConfirmedDomainEvent transforms the booking information correctly`() {
    val expectedArrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking =
      BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withArrivalDate(expectedArrivalDateTime.atZone(ZoneOffset.UTC).toLocalDate())
        .produce()

    val event = cas3DomainEventBuilder.getBookingConfirmedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.expectedArrivedAt == expectedArrivalDateTime &&
        data.notes == "" &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.confirmedBy!!.staffCode == user.deliusStaffCode &&
        data.confirmedBy!!.username == user.deliusUsername &&
        data.confirmedBy!!.probationRegionCode == user.probationRegion.deliusCode &&
        it.data.eventType == EventType.bookingConfirmed
    }
  }

  @Test
  fun `getBookingProvisionallyMadeDomainEvent transforms the booking information correctly`() {
    val expectedArrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking =
      BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withArrivalDate(expectedArrivalDateTime.atZone(ZoneOffset.UTC).toLocalDate())
        .produce()

    val event = cas3DomainEventBuilder.getBookingProvisionallyMadeDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.expectedArrivedAt == expectedArrivalDateTime &&
        data.notes == "" &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.bookedBy?.staffCode == user.deliusStaffCode &&
        data.bookedBy?.username == user.deliusUsername &&
        data.bookedBy?.probationRegionCode == user.probationRegion.deliusCode &&
        it.data.eventType == EventType.bookingProvisionallyMade
    }
  }

  @Test
  fun `getPersonArrivedDomainEvent transforms the booking and arrival information correctly`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    booking.arrivals += arrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.getPersonArrivedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.premises.addressLine1 == premises.addressLine1 &&
        data.premises.addressLine2 == premises.addressLine2 &&
        data.premises.postcode == premises.postcode &&
        data.premises.town == premises.town &&
        data.premises.region == premises.probationRegion.name &&
        data.arrivedAt == arrivalDateTime &&
        data.expectedDepartureOn == expectedDepartureDate &&
        data.notes == notes &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.recordedBy!!.staffCode == user.deliusStaffCode &&
        data.recordedBy!!.username == user.deliusUsername &&
        data.recordedBy!!.probationRegionCode == user.probationRegion.deliusCode &&
        it.data.eventType == EventType.personArrived
    }
  }

  @Test
  fun `getPersonArrivedDomainEvent transforms the booking and arrival information correctly without staff detail`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    booking.arrivals += arrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.getPersonArrivedDomainEvent(booking, null)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.noms == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.arrivedAt == arrivalDateTime &&
        data.expectedDepartureOn == expectedDepartureDate &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.recordedBy == null &&
        it.data.eventType == EventType.personArrived
    }
  }

  @Test
  fun `getPersonDepartedDomainEvent transforms the booking and departure information correctly`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    val reason = departureReasonEntity(reasonName)

    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += departureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.getPersonDepartedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.premises.addressLine1 == premises.addressLine1 &&
        data.premises.addressLine2 == premises.addressLine2 &&
        data.premises.postcode == premises.postcode &&
        data.premises.town == premises.town &&
        data.premises.region == premises.probationRegion.name &&
        data.departedAt == departureDateTime.toInstant() &&
        data.reason == reasonName &&
        data.notes == notes &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        it.data.eventType == EventType.personDeparted
    }
    assertStaffDetails(event.data.eventDetails.recordedBy, user)
  }

  @Test
  fun `getPersonDepartedDomainEvent transforms the booking and departure information correctly without staff detail`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    val reason = departureReasonEntity(reasonName)

    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += departureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.getPersonDepartedDomainEvent(booking, null)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.departedAt == departureDateTime.toInstant() &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        it.data.eventType == EventType.personDeparted
    }
    assertThat(event.data.eventDetails.recordedBy).isNull()
  }

  @Test
  fun `getReferralSubmittedDomainEvent transforms the application correctly`() {
    val probationRegion = probationRegionEntity()

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val event = cas3DomainEventBuilder.getReferralSubmittedDomainEvent(application)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == null &&
        it.crn == application.crn &&
        it.nomsNumber == application.nomsNumber &&
        data.personReference.crn == application.crn &&
        data.personReference.noms == application.nomsNumber &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        it.data.eventType == EventType.referralSubmitted
    }
  }

  @Test
  fun `buildDepartureUpdatedDomainEvent transforms the booking and departure information correctly`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    val reason = departureReasonEntity(reasonName)

    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += departureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.buildDepartureUpdatedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails
      assertBookingEventData(it, booking, premises.id) &&
        assertPremisesEventData(it, premises) &&
        assertTemporaryAccommodationApplicationEventData(it, application) &&
        data.departedAt == departureDateTime.toInstant() &&
        data.reason == reasonName &&
        data.notes == notes &&
        it.data.eventType == EventType.personDepartureUpdated
    }
  }

  @Test
  fun `getBookingCancelledUpdatedsDomainEvent transforms the booking information correctly`() {
    val cancellationReasonName = "Some cancellation reason"
    val cancellationNotes = "Some notes about the cancellation"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    val cancellationReason = cancellationReasonEntity(cancellationReasonName)

    booking.cancellations += cancellationEntity(booking, cancellationReason, cancellationNotes)

    val event = cas3DomainEventBuilder.getBookingCancelledUpdatedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.cancellationReason == cancellationReasonName &&
        data.notes == cancellationNotes &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.cancelledAt == booking.cancellation?.date &&
        it.data.eventType == EventType.bookingCancelledUpdated
    }
  }

  @Test
  fun `getPersonArrivedUpdatedDomainEvent transforms the booking and arrival updated information correctly`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    booking.arrivals += arrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.buildPersonArrivedUpdatedDomainEvent(booking, user)

    assertThat(event).matches {
      val data = it.data.eventDetails
      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        assertCAS3PersonArrivedUpdatedEventPremisesEventData(it, premises) &&
        data.arrivedAt == arrivalDateTime &&
        data.expectedDepartureOn == expectedDepartureDate &&
        data.notes == notes &&
        it.data.eventType == EventType.personArrivedUpdated
    }
    assertStaffDetails(event.data.eventDetails.recordedBy, user)
  }

  @Test
  fun `getPersonArrivedUpdatedDomainEvent transforms the booking and arrival updated information correctly without staff detail`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()

    val premises = temporaryAccommodationPremisesEntity(probationRegion)

    val user = userEntity(probationRegion)

    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = bookingEntity(premises, application)

    booking.arrivals += arrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.buildPersonArrivedUpdatedDomainEvent(booking, null)

    assertThat(event).matches {
      val data = it.data.eventDetails
      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        it.nomsNumber == booking.nomsNumber &&
        data.deliusEventNumber == application.eventNumber &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}" &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        assertCAS3PersonArrivedUpdatedEventPremisesEventData(it, premises) &&
        data.arrivedAt == arrivalDateTime &&
        data.expectedDepartureOn == expectedDepartureDate &&
        data.notes == notes &&
        it.data.eventType == EventType.personArrivedUpdated
    }
  }

  private fun departureEntity(
    booking: BookingEntity,
    departureDateTime: OffsetDateTime,
    reason: DepartureReasonEntity,
    moveOnCategory: MoveOnCategoryEntity,
    notes: String,
  ) = DepartureEntityFactory()
    .withBooking(booking)
    .withDateTime(departureDateTime)
    .withReason(reason)
    .withMoveOnCategory(moveOnCategory)
    .withNotes(notes)
    .produce()

  private fun moveOnCategoryEntity(
    moveOnCategoryDescription: String,
    moveOnCategoryLabel: String,
  ) = MoveOnCategoryEntityFactory()
    .withName(moveOnCategoryDescription)
    .withLegacyDeliusCategoryCode(moveOnCategoryLabel)
    .produce()

  private fun departureReasonEntity(reasonName: String) = DepartureReasonEntityFactory()
    .withName(reasonName)
    .produce()

  private fun arrivalEntity(
    booking: BookingEntity,
    arrivalDateTime: Instant,
    expectedDepartureDate: LocalDate,
    notes: String,
  ) = ArrivalEntityFactory()
    .withBooking(booking)
    .withArrivalDateTime(arrivalDateTime)
    .withExpectedDepartureDate(expectedDepartureDate)
    .withNotes(notes)
    .produce()

  private fun bookingEntity(
    premises: TemporaryAccommodationPremisesEntity,
    application: TemporaryAccommodationApplicationEntity,
  ) = BookingEntityFactory()
    .withPremises(premises)
    .withApplication(application)
    .produce()

  private fun temporaryAccommodationApplicationEntity(
    user: UserEntity,
    probationRegion: ProbationRegionEntity,
  ) = TemporaryAccommodationApplicationEntityFactory()
    .withCreatedByUser(user)
    .withProbationRegion(probationRegion)
    .produce()

  private fun userEntity(probationRegion: ProbationRegionEntity) = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private fun temporaryAccommodationPremisesEntity(probationRegion: ProbationRegionEntity) = TemporaryAccommodationPremisesEntityFactory()
    .withProbationRegion(probationRegion)
    .withLocalAuthorityArea(
      LocalAuthorityEntityFactory().produce(),
    ).produce()

  private fun probationRegionEntity() = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory().produce(),
    ).produce()

  private fun cancellationReasonEntity(cancellationReasonName: String) = CancellationReasonEntityFactory()
    .withName(cancellationReasonName)
    .produce()

  private fun cancellationEntity(
    booking: BookingEntity,
    cancellationReason: CancellationReasonEntity,
    cancellationNotes: String,
  ) = CancellationEntityFactory()
    .withBooking(booking)
    .withReason(cancellationReason)
    .withNotes(cancellationNotes)
    .produce()

  private fun assertCAS3PersonArrivedUpdatedEventPremisesEventData(
    eventData: DomainEvent<CAS3PersonArrivedUpdatedEvent>,
    premises: PremisesEntity,
  ): Boolean {
    val data = eventData.data.eventDetails
    return data.premises.addressLine1 == premises.addressLine1 &&
      data.premises.addressLine2 == premises.addressLine2 &&
      data.premises.postcode == premises.postcode &&
      data.premises.town == premises.town &&
      data.premises.region == premises.probationRegion.name
  }

  private fun assertStaffDetails(
    staffMember: StaffMember?,
    user: UserEntity?,
  ): Boolean = staffMember!!.staffCode == user!!.deliusStaffCode &&
    staffMember.username == user.deliusUsername &&
    staffMember.probationRegionCode == user.probationRegion.deliusCode

  private fun assertBookingEventData(
    eventData: DomainEvent<CAS3PersonDepartureUpdatedEvent>,
    booking: BookingEntity,
    premisesId: UUID,
  ): Boolean {
    val data = eventData.data.eventDetails

    return eventData.bookingId == booking.id &&
      eventData.crn == booking.crn &&
      eventData.nomsNumber == booking.nomsNumber &&
      data.personReference.crn == booking.crn &&
      data.personReference.noms == booking.nomsNumber &&
      data.bookingId == booking.id &&
      data.bookingUrl.toString() == "http://api/premises/$premisesId/bookings/${booking.id}"
  }

  private fun assertPremisesEventData(
    eventData: DomainEvent<CAS3PersonDepartureUpdatedEvent>,
    premises: PremisesEntity,
  ): Boolean {
    val data = eventData.data.eventDetails
    return data.premises.addressLine1 == premises.addressLine1 &&
      data.premises.addressLine2 == premises.addressLine2 &&
      data.premises.postcode == premises.postcode &&
      data.premises.town == premises.town &&
      data.premises.region == premises.probationRegion.name
  }

  private fun assertTemporaryAccommodationApplicationEventData(
    eventData: DomainEvent<CAS3PersonDepartureUpdatedEvent>,
    application: TemporaryAccommodationApplicationEntity,
  ): Boolean {
    val data = eventData.data.eventDetails
    return data.deliusEventNumber == application.eventNumber &&
      data.applicationId == application.id &&
      data.applicationUrl.toString() == "http://api/applications/${application.id}"
  }
}
