package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DomainEventBuilderTest {
  private val domainEventBuilder = DomainEventBuilder(
    applicationUrlTemplate = "http://api/applications/#applicationId",
    bookingUrlTemplate = "http://api/premises/#premisesId/bookings/#bookingId",
  )

  @Test
  fun `getBookingCancelledDomainEvent transforms the booking information correctly`() {
    val cancellationReasonName = "Some cancellation reason"
    val cancellationNotes = "Some notes about the cancellation"

    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      )
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory().produce(),
      )
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withApplication(application)
      .produce()

    val cancellationReason = CancellationReasonEntityFactory()
      .withName(cancellationReasonName)
      .produce()

    booking.cancellations += CancellationEntityFactory()
      .withBooking(booking)
      .withReason(cancellationReason)
      .withNotes(cancellationNotes)
      .produce()

    val event = domainEventBuilder.getBookingCancelledDomainEvent(booking)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.cancellationReason == cancellationReasonName &&
        data.cancellationContext == cancellationNotes &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}"
    }
  }

  @Test
  fun `getBookingProvisionallyMadeDomainEvent transforms the booking information correctly`() {
    val expectedArrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")

    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      )
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory().produce(),
      )
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withApplication(application)
      .withArrivalDate(expectedArrivalDateTime.atZone(ZoneOffset.UTC).toLocalDate())
      .produce()

    val event = domainEventBuilder.getBookingProvisionallyMadeDomainEvent(booking)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
        data.personReference.crn == booking.crn &&
        data.personReference.noms == booking.nomsNumber &&
        data.bookingId == booking.id &&
        data.bookingUrl.toString() == "http://api/premises/${premises.id}/bookings/${booking.id}" &&
        data.expectedArrivedAt == expectedArrivalDateTime &&
        data.notes == "" &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}"
    }
  }

  @Test
  fun `getPersonArrivedDomainEvent transforms the booking and arrival information correctly`() {
    val arrivalDateTime = Instant.parse("2023-07-15T00:00:00Z")
    val expectedDepartureDate = LocalDate.parse("2023-10-15")
    val notes = "Some notes about the arrival"

    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      )
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory().produce(),
      )
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withApplication(application)
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withBooking(booking)
      .withArrivalDateTime(arrivalDateTime)
      .withExpectedDepartureDate(expectedDepartureDate)
      .withNotes(notes)
      .produce()

    val event = domainEventBuilder.getPersonArrivedDomainEvent(booking)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
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
        data.applicationUrl.toString() == "http://api/applications/${application.id}"
    }
  }

  @Test
  fun `getPersonDepartedDomainEvent transforms the booking and departure information correctly`() {
    val departureDateTime = OffsetDateTime.parse("2023-07-15T00:00:00Z")
    val reasonName = "Returned to custody"
    val notes = "Some notes about the departure"
    val moveOnCategoryDescription = "Returned to custody"
    val moveOnCategoryLabel = "RTC"

    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      )
      .produce()

    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory().produce(),
      )
      .produce()

    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withCreatedByUser(user)
      .withProbationRegion(probationRegion)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withApplication(application)
      .produce()

    val reason = DepartureReasonEntityFactory()
      .withName(reasonName)
      .produce()

    val moveOnCategory = MoveOnCategoryEntityFactory()
      .withName(moveOnCategoryDescription)
      .withLegacyDeliusCategoryCode(moveOnCategoryLabel)
      .produce()

    booking.departures += DepartureEntityFactory()
      .withBooking(booking)
      .withDateTime(departureDateTime)
      .withReason(reason)
      .withMoveOnCategory(moveOnCategory)
      .withNotes(notes)
      .produce()

    val event = domainEventBuilder.getPersonDepartedDomainEvent(booking)

    assertThat(event).matches {
      val data = it.data.eventDetails

      it.applicationId == application.id &&
        it.bookingId == booking.id &&
        it.crn == booking.crn &&
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
        data.moveOnCategory.description == moveOnCategoryDescription &&
        data.moveOnCategory.label == moveOnCategoryLabel &&
        data.applicationId == application.id &&
        data.applicationUrl.toString() == "http://api/applications/${application.id}"
    }
  }
}
