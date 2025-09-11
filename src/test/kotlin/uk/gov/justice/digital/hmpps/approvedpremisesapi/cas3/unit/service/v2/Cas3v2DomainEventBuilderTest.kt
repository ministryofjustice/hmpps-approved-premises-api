package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("CyclomaticComplexMethod")
class Cas3v2DomainEventBuilderTest {
  private val cas3DomainEventBuilder =
    Cas3v2DomainEventBuilder(
      applicationUrlTemplate = "http://api/applications/#applicationId",
      bookingUrlTemplate = "http://api/premises/#premisesId/bookings/#bookingId",
    )

  @Test
  fun `getBookingCancelledDomainEvent transforms the booking information correctly`() {
    val cancellationReasonName = "Some cancellation reason"
    val cancellationNotes = "Some notes about the cancellation"
    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(premises, application)
    val cancellationReason = cancellationReasonEntity(cancellationReasonName)
    booking.cancellations += cas3CancellationEntity(booking, cancellationReason, cancellationNotes)

    val event = cas3DomainEventBuilder.getBookingCancelledDomainEvent(booking, user)
    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.cancellationReason).isEqualTo(cancellationReasonName)
      assertThat(data.notes).isEqualTo(cancellationNotes)
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(data.cancelledAt).isEqualTo(booking.cancellation?.date)
      assertThat(data.cancelledBy!!.staffCode).isEqualTo(user.deliusStaffCode)
      assertThat(data.cancelledBy!!.username).isEqualTo(user.deliusUsername)
      assertThat(data.cancelledBy!!.probationRegionCode).isEqualTo(user.probationRegion.deliusCode)
      assertThat(event.data.eventType).isEqualTo(EventType.bookingCancelled)
    })
  }

  @Test
  fun `getBookingConfirmedDomainEvent transforms the booking information correctly`() {
    val expectedArrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(
      premises,
      application,
      arrivalDate = expectedArrivalDateTime.toLocalDate(),
    )

    val event = cas3DomainEventBuilder.getBookingConfirmedDomainEvent(booking, user)

    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.expectedArrivedAt).isEqualTo(expectedArrivalDateTime)
      assertThat(data.notes).isEqualTo("")
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(data.confirmedBy?.staffCode).isEqualTo(user.deliusStaffCode)
      assertThat(data.confirmedBy?.username).isEqualTo(user.deliusUsername)
      assertThat(data.confirmedBy?.probationRegionCode).isEqualTo(user.probationRegion.deliusCode)
      assertThat(event.data.eventType).isEqualTo(EventType.bookingConfirmed)
    })
  }

  @Test
  fun `getPremisesUnarchiveEvent transforms the premises information correctly to a domain event`() {
    val currentStartDate = LocalDate.now().minusDays(20)
    val currentEndDate = LocalDate.now().minusDays(10)
    val newStartDate = LocalDate.now().plusDays(5)
    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    premises.endDate = currentEndDate
    val user = userEntity(probationRegion)

    val event = cas3DomainEventBuilder.getPremisesUnarchiveEvent(premises, currentStartDate, newStartDate, currentEndDate, user)

    assertAll({
      assertThat(event.applicationId).isNull()
      assertThat(event.bookingId).isNull()
      assertThat(event.crn).isNull()
      assertThat(event.nomsNumber).isNull()
      assertThat(event.data.eventType).isEqualTo(EventType.premisesUnarchived)
      assertThat(event.data.eventDetails.premisesId).isEqualTo(premises.id)
      assertThat(event.data.eventDetails.userId).isEqualTo(user.id)
      assertThat(event.data.eventDetails.currentStartDate).isEqualTo(currentStartDate)
      assertThat(event.data.eventDetails.newStartDate).isEqualTo(newStartDate)
      assertThat(event.occurredAt).isWithinTheLastMinute()
      assertThat(event.data.timestamp).isWithinTheLastMinute()
      assertThat(event.id).isEqualTo(event.data.id)
    })
  }

  @Test
  fun `getPersonArrivedDomainEvent transforms the booking and arrival information correctly`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = cas3BookingEntity(premises, application)
    booking.arrivals += cas3ArrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.getPersonArrivedDomainEvent(booking, user)

    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.premises.addressLine1).isEqualTo(premises.addressLine1)
      assertThat(data.premises.addressLine2).isEqualTo(premises.addressLine2)
      assertThat(data.premises.postcode).isEqualTo(premises.postcode)
      assertThat(data.premises.town).isEqualTo(premises.town)
      assertThat(data.premises.region).isEqualTo(premises.probationDeliveryUnit.probationRegion.name)
      assertThat(data.arrivedAt).isEqualTo(arrivalDateTime)
      assertThat(data.expectedDepartureOn).isEqualTo(expectedDepartureDate)
      assertThat(data.notes).isEqualTo(notes)
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(data.recordedBy!!.staffCode).isEqualTo(user.deliusStaffCode)
      assertThat(data.recordedBy!!.username).isEqualTo(user.deliusUsername)
      assertThat(data.recordedBy!!.probationRegionCode).isEqualTo(user.probationRegion.deliusCode)
      assertThat(event.data.eventType).isEqualTo(EventType.personArrived)
    })
  }

  @Test
  fun `getPersonArrivedDomainEvent transforms the booking and arrival information correctly without staff detail`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)

    val booking = cas3BookingEntity(premises, application)
    booking.arrivals += cas3ArrivalEntity(booking, arrivalDateTime, expectedDepartureDate, notes)

    val event = cas3DomainEventBuilder.getPersonArrivedDomainEvent(booking, null)

    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.arrivedAt).isEqualTo(arrivalDateTime)
      assertThat(data.expectedDepartureOn).isEqualTo(expectedDepartureDate)
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(data.recordedBy).isNull()
      assertThat(event.data.eventType).isEqualTo(EventType.personArrived)
    })
  }

  @Test
  fun `getPersonDepartedDomainEvent transforms the booking and departure information correctly`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(premises, application)
    val reason = departureReasonEntity(reasonName)
    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += cas3DepartureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.getPersonDepartedDomainEvent(booking, user)

    val data = event.data.eventDetails
    assertAll({
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.premises.addressLine1).isEqualTo(premises.addressLine1)
      assertThat(data.premises.addressLine2).isEqualTo(premises.addressLine2)
      assertThat(data.premises.postcode).isEqualTo(premises.postcode)
      assertThat(data.premises.town).isEqualTo(premises.town)
      assertThat(data.premises.region).isEqualTo(premises.probationDeliveryUnit.probationRegion.name)
      assertThat(data.departedAt).isEqualTo(departureDateTime.toInstant())
      assertThat(data.reason).isEqualTo(reasonName)
      assertThat(data.notes).isEqualTo(notes)
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(event.data.eventType).isEqualTo(EventType.personDeparted)
    })
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
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(premises, application)
    val reason = departureReasonEntity(reasonName)
    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += cas3DepartureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.getPersonDepartedDomainEvent(booking, null)

    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.departedAt).isEqualTo(departureDateTime.toInstant())
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(event.data.eventType).isEqualTo(EventType.personDeparted)
    })
    assertThat(event.data.eventDetails.recordedBy).isNull()
  }

  @Test
  fun `buildDepartureUpdatedDomainEvent transforms the booking and departure information correctly`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(premises, application)
    val reason = departureReasonEntity(reasonName)
    val moveOnCategory = moveOnCategoryEntity(moveOnCategoryDescription, moveOnCategoryLabel)

    booking.departures += cas3DepartureEntity(booking, departureDateTime, reason, moveOnCategory, notes)

    val event = cas3DomainEventBuilder.buildDepartureUpdatedDomainEvent(booking, user)

    val data = event.data.eventDetails
    assertBookingEventData(event, booking, premises.id)
    assertPremisesEventData(event, premises)
    assertTemporaryAccommodationApplicationEventData(event, application)
    assertAll({
      assertThat(data.departedAt).isEqualTo(departureDateTime.toInstant())
      assertThat(data.reason).isEqualTo(reasonName)
      assertThat(data.notes).isEqualTo(notes)
      assertThat(event.data.eventType).isEqualTo(EventType.personDepartureUpdated)
    })
  }

  @Test
  fun `getBookingCancelledUpdatedsDomainEvent transforms the cas3 booking information correctly`() {
    val cancellationReasonName = "Some cancellation reason"
    val cancellationNotes = "Some notes about the cancellation"
    val probationRegion = probationRegionEntity()
    val premises = cas3PremisesEntity(probationRegion)
    val user = userEntity(probationRegion)
    val application = temporaryAccommodationApplicationEntity(user, probationRegion)
    val booking = cas3BookingEntity(premises, application)
    val cancellationReason = cancellationReasonEntity(cancellationReasonName)
    booking.cancellations += cas3CancellationEntity(booking, cancellationReason, cancellationNotes)

    val event = cas3DomainEventBuilder.getBookingCancelledUpdatedDomainEvent(booking, user)
    val data = event.data.eventDetails
    assertAll({
      assertThat(event.applicationId).isEqualTo(application.id)
      assertThat(event.bookingId).isEqualTo(booking.id)
      assertThat(event.crn).isEqualTo(booking.crn)
      assertThat(event.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/${premises.id}/bookings/${booking.id}")
      assertThat(data.cancellationReason).isEqualTo(cancellationReasonName)
      assertThat(data.notes).isEqualTo(cancellationNotes)
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl.toString()).isEqualTo("http://api/applications/${application.id}")
      assertThat(data.cancelledAt).isEqualTo(booking.cancellation?.date)
      assertThat(event.data.eventType).isEqualTo(EventType.bookingCancelledUpdated)
    })
  }

  private fun cas3DepartureEntity(
    booking: Cas3BookingEntity,
    departureDateTime: OffsetDateTime,
    reason: DepartureReasonEntity,
    moveOnCategory: MoveOnCategoryEntity,
    notes: String,
  ) = Cas3DepartureEntityFactory()
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

  private fun cas3ArrivalEntity(
    booking: Cas3BookingEntity,
    arrivalDateTime: Instant,
    expectedDepartureDate: LocalDate,
    notes: String,
  ) = Cas3ArrivalEntityFactory()
    .withBooking(booking)
    .withArrivalDateTime(arrivalDateTime)
    .withExpectedDepartureDate(expectedDepartureDate)
    .withNotes(notes)
    .produce()

  private fun cas3BookingEntity(
    premises: Cas3PremisesEntity,
    application: TemporaryAccommodationApplicationEntity,
    arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  ) = Cas3BookingEntityFactory()
    .withPremises(premises)
    .withApplication(application)
    .withArrivalDate(arrivalDate)
    .withBedspace(
      Cas3BedspaceEntityFactory()
        .withPremises(premises)
        .produce(),
    )
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

  private fun cas3PremisesEntity(probationRegion: ProbationRegionEntity) = Cas3PremisesEntityFactory()
    .withProbationDeliveryUnit(
      ProbationDeliveryUnitEntityFactory()
        .withProbationRegion(probationRegion)
        .produce(),
    )
    .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
    .produce()

  private fun probationRegionEntity() = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory().produce(),
    ).produce()

  private fun cancellationReasonEntity(cancellationReasonName: String) = CancellationReasonEntityFactory()
    .withName(cancellationReasonName)
    .produce()

  private fun cas3CancellationEntity(
    booking: Cas3BookingEntity,
    cancellationReason: CancellationReasonEntity,
    cancellationNotes: String,
  ) = Cas3CancellationEntityFactory()
    .withBooking(booking)
    .withReason(cancellationReason)
    .withNotes(cancellationNotes)
    .produce()

  private fun assertStaffDetails(
    staffMember: StaffMember?,
    user: UserEntity?,
  ): Boolean = staffMember!!.staffCode == user!!.deliusStaffCode &&
    staffMember.username == user.deliusUsername &&
    staffMember.probationRegionCode == user.probationRegion.deliusCode

  private fun assertBookingEventData(
    eventData: DomainEvent<CAS3PersonDepartureUpdatedEvent>,
    booking: Cas3BookingEntity,
    premisesId: UUID,
  ) {
    val data = eventData.data.eventDetails
    assertAll({
      assertThat(eventData.bookingId).isEqualTo(booking.id)
      assertThat(eventData.crn).isEqualTo(booking.crn)
      assertThat(eventData.nomsNumber).isEqualTo(booking.nomsNumber)
      assertThat(data.personReference.crn).isEqualTo(booking.crn)
      assertThat(data.personReference.noms).isEqualTo(booking.nomsNumber)
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.bookingUrl.toString()).isEqualTo("http://api/premises/$premisesId/bookings/${booking.id}")
    })
  }

  private fun assertPremisesEventData(
    eventData: DomainEvent<CAS3PersonDepartureUpdatedEvent>,
    premises: Cas3PremisesEntity,
  ) {
    val data = eventData.data.eventDetails
    assertAll({
      assertThat(data.premises.addressLine1).isEqualTo(premises.addressLine1)
      assertThat(data.premises.addressLine2).isEqualTo(premises.addressLine2)
      assertThat(data.premises.region).isEqualTo(premises.probationDeliveryUnit.probationRegion.name)
    })
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
