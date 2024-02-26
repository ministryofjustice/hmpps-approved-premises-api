package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class DomainEventDescriberTest {
  private val mockDomainEventService = mockk<DomainEventService>()

  private val domainEventDescriber = DomainEventDescriber(mockDomainEventService)

  @Test
  fun `Returns expected description for application submitted event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was submitted")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted", "rejected"])
  fun `Returns expected description for application assessed event`(decision: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

    every { mockDomainEventService.getApplicationAssessedDomainEvent(any()) } returns buildDomainEvent {
      ApplicationAssessedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.application.assessed",
        eventDetails = ApplicationAssessedFactory()
          .withDecision(decision)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was assessed and $decision")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01,2024-04-01", "2024-01-02,2024-04-02"])
  fun `Returns expected description for booking made event`(arrivalDate: LocalDate, departureDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)

    every { mockDomainEventService.getBookingMadeEvent(any()) } returns buildDomainEvent {
      BookingMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.made",
        eventDetails = BookingMadeFactory()
          .withArrivalOn(arrivalDate)
          .withDepartureOn(departureDate)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A booking was made for between $arrivalDate and $departureDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person arrived event`(arrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)

    every { mockDomainEventService.getPersonArrivedEvent(any()) } returns buildDomainEvent {
      PersonArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.arrived",
        eventDetails = PersonArrivedFactory()
          .withArrivedAt(arrivalDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved into the premises on $arrivalDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person not arrived event`(expectedArrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

    every { mockDomainEventService.getPersonNotArrivedEvent(any()) } returns buildDomainEvent {
      PersonNotArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.not-arrived",
        eventDetails = PersonNotArrivedFactory()
          .withExpectedArrivalOn(expectedArrivalDate)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person was due to move into the premises on $expectedArrivalDate but did not arrive")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-04-01", "2024-04-02"])
  fun `Returns expected description for person departed event`(departureDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)

    every { mockDomainEventService.getPersonDepartedEvent(any()) } returns buildDomainEvent {
      PersonDepartedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.person.departed",
        eventDetails = PersonDepartedFactory()
          .withDepartedAt(departureDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved out of the premises on $departureDate")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking not made event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      BookingNotMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.not-made",
        eventDetails = BookingNotMadeFactory()
          .withFailureDescription(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A booking was not made for the placement request. The reason was: $reason")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking cancelled event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)

    every { mockDomainEventService.getBookingCancelledEvent(any()) } returns buildDomainEvent {
      BookingCancelledEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.booking.cancelled",
        eventDetails = BookingCancelledFactory()
          .withCancellationReason(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The booking was cancelled. The reason was: $reason")
  }

  @Test
  fun `Returns expected description for booking changed event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The booking had its arrival or departure date changed")
  }

  @Test
  fun `Returns expected description for application withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was withdrawn")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted,Reason A", "rejected,Reason B"])
  fun `Returns expected description for assessment appealed event`(decision: String, reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)

    every { mockDomainEventService.getAssessmentAppealedEvent(any()) } returns buildDomainEvent {
      AssessmentAppealedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.assessment.appealed",
        eventDetails = AssessmentAppealedFactory()
          .withDecision(AppealDecision.valueOf(decision))
          .withDecisionDetail(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The assessment was appealed and $decision. The reason was: $reason")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with no dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.placement-application.withdrawn",
        eventDetails = PlacementApplicationWithdrawnFactory()
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A request for placement was withdrawn")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with placement dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.placement-application.withdrawn",
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withPlacementDates(
            listOf(
              DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)),
              DatePeriod(LocalDate.of(2024, 5, 6), LocalDate.of(2024, 7, 8)),
            ),
          )
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024, Monday 6 May 2024 to Monday 8 July 2024")
  }

  @Test
  fun `Returns expected description for match request withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

    every { mockDomainEventService.getMatchRequestWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      MatchRequestWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = "approved-premises.match-request.withdrawn",
        eventDetails = MatchRequestWithdrawnFactory()
          .withDatePeriod(DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024")
  }

  private fun <T> buildDomainEvent(builder: (UUID) -> T): DomainEvent<T> {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    return DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = "SOME-CRN",
      occurredAt = Instant.now(),
      data = builder(id),
    )
  }
}

data class DomainEventSummaryImpl(
  override val id: String,
  override val type: DomainEventType,
  override val occurredAt: Timestamp,
  override val applicationId: UUID?,
  override val assessmentId: UUID?,
  override val bookingId: UUID?,
  override val premisesId: UUID?,
  override val appealId: UUID?,
) : DomainEventSummary {
  companion object {
    fun ofType(type: DomainEventType) = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = type,
      occurredAt = Timestamp.from(Instant.now()),
      applicationId = null,
      assessmentId = null,
      bookingId = null,
      premisesId = null,
      appealId = null,
    )
  }
}
