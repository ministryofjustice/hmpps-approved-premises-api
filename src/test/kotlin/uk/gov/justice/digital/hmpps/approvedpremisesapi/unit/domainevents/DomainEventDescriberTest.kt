package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.domainevents.DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class DomainEventDescriberTest {
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockAssessmentClarificationNoteRepository = mockk<AssessmentClarificationNoteRepository>()

  private val domainEventDescriber = DomainEventDescriber(mockDomainEventService, mockAssessmentClarificationNoteRepository)

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
        eventType = EventType.applicationAssessed,
        eventDetails = ApplicationAssessedFactory()
          .withDecision(decision)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was assessed and $decision")
  }

  @Test
  fun `Returns expected description for booking made event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { mockDomainEventService.getBookingMadeEvent(any()) } returns buildDomainEvent {
      BookingMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingMade,
        eventDetails = BookingMadeFactory()
          .withArrivalOn(arrivalDate)
          .withDepartureOn(departureDate)
          .withPremises(
            EventPremisesFactory()
              .withName("The Premises Name")
              .produce(),
          )
          .withDeliusEventNumber("989")
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A placement at The Premises Name was booked for Monday 1 January 2024 to Monday 1 April 2024 against Delius Event Number 989")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person arrived event`(arrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)

    every { mockDomainEventService.getPersonArrivedEvent(any()) } returns buildDomainEvent {
      PersonArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personArrived,
        eventDetails = PersonArrivedFactory()
          .withArrivedAt(arrivalDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved into the premises on ${arrivalDate.toUiFormat()}")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person not arrived event`(expectedArrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

    every { mockDomainEventService.getPersonNotArrivedEvent(any()) } returns buildDomainEvent {
      PersonNotArrivedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personNotArrived,
        eventDetails = PersonNotArrivedFactory()
          .withExpectedArrivalOn(expectedArrivalDate)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person was due to move into the premises on ${expectedArrivalDate.toUiFormat()} but did not arrive")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-04-01", "2024-04-02"])
  fun `Returns expected description for person departed event`(departureDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)

    every { mockDomainEventService.getPersonDepartedEvent(any()) } returns buildDomainEvent {
      PersonDepartedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personDeparted,
        eventDetails = PersonDepartedFactory()
          .withDepartedAt(departureDate.atTime(12, 34, 56).toInstant(ZoneOffset.UTC))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The person moved out of the premises on ${departureDate.toUiFormat()}")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking not made event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      BookingNotMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingNotMade,
        eventDetails = BookingNotMadeFactory()
          .withFailureDescription(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A placement was not made for the placement request. The reason was: $reason")
  }

  @Test
  fun `Returns expected description for booking not made event with no failure description`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
    val bookingNotMade = BookingNotMadeFactory().produce().copy(failureDescription = null)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      BookingNotMadeEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingNotMade,
        eventDetails = bookingNotMade,
      )
    }
    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A placement was not made for the placement request.")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking cancelled event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)

    every { mockDomainEventService.getBookingCancelledEvent(any()) } returns buildDomainEvent {
      BookingCancelledEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingCancelled,
        eventDetails = BookingCancelledFactory()
          .withCancellationReason(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The placement was cancelled. The reason was: '$reason'")
  }

  @Test
  fun `Returns expected description for booking changed event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)

    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { mockDomainEventService.getBookingChangedEvent(any()) } returns buildDomainEvent {
      BookingChangedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingChanged,
        eventDetails = BookingChangedFactory()
          .withArrivalOn(arrivalDate)
          .withDepartureOn(departureDate)
          .withPremises(
            EventPremisesFactory()
              .withName("The Premises Name")
              .produce(),
          )
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A placement at The Premises Name had its arrival and/or departure date changed to Monday 1 January 2024 to Monday 1 April 2024")
  }

  @Test
  fun `Returns expected description for application withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      ApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("change_in_circumstances_new_application_to_be_submitted")
          .withOtherWithdrawalReason(null)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was withdrawn. The reason was: 'change in circumstances new application to be submitted'")
  }

  @Test
  fun `Returns expected description for application withdrawn event with additional reason`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      ApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("the main withdrawal reason")
          .withOtherWithdrawalReason("additional reason")
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The application was withdrawn. The reason was: 'the main withdrawal reason' (additional reason)")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted,Reason A", "rejected,Reason B"])
  fun `Returns expected description for assessment appealed event`(decision: String, reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)

    every { mockDomainEventService.getAssessmentAppealedEvent(any()) } returns buildDomainEvent {
      AssessmentAppealedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.assessmentAppealed,
        eventDetails = AssessmentAppealedFactory()
          .withDecision(AppealDecision.valueOf(decision))
          .withDecisionDetail(reason)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("The assessment was appealed and $decision. The reason was: $reason")
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      ",,The assessment was automatically allocated to an unknown user",
      "Alan,,The assessment was automatically allocated to Alan A",
      ",Brenda,The assessment was allocated to an unknown user by Brenda B",
      "Carol,Derek,The assessment was allocated to Carol C by Derek D",
    ],
  )
  fun `Returns expected description for assessment allocated event`(
    allocatedTo: String?,
    allocatedBy: String?,
    expectedDescription: String,
  ) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED)

    every { mockDomainEventService.getAssessmentAllocatedEvent(any()) } returns buildDomainEvent {
      AssessmentAllocatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.assessmentAllocated,
        eventDetails = AssessmentAllocatedFactory()
          .withAllocatedTo(
            allocatedTo?.let { allocatedTo ->
              StaffMemberFactory()
                .withForenames(allocatedTo)
                .withSurname(allocatedTo.first().toString())
                .produce()
            },
          )
          .withAllocatedBy(
            allocatedBy?.let { allocatedBy ->
              StaffMemberFactory()
                .withForenames(allocatedBy)
                .withSurname(allocatedBy.first().toString())
                .produce()
            },
          )
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(expectedDescription)
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with no dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.placementApplicationWithdrawn,
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withWithdrawalReason(PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN.toString())
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("A request for placement was withdrawn. The reason was: 'Related application withdrawn'")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with placement dates`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.placementApplicationWithdrawn,
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withWithdrawalReason(PlacementApplicationWithdrawalReason.DUPLICATE_PLACEMENT_REQUEST.toString())
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

    assertThat(result).isEqualTo(
      "A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024, Monday 6 May 2024 to Monday 8 July 2024. " +
        "The reason was: 'Duplicate placement request'",
    )
  }

  @Test
  fun `Returns expected description for match request withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

    every { mockDomainEventService.getMatchRequestWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      MatchRequestWithdrawnEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.matchRequestWithdrawn,
        eventDetails = MatchRequestWithdrawnFactory()
          .withWithdrawalReason(PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION.toString())
          .withDatePeriod(DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)))
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024. " +
        "The reason was: 'No capacity due to placement prioritisation'",
    )
  }

  @ParameterizedTest
  @CsvSource(
    "rotl,Release on Temporary Licence (ROTL)",
    "releaseFollowingDecisions,Release directed following parole board or other hearing/decision",
    "additionalPlacement,An additional placement on an existing application",
  )
  fun `Returns expected description for request for placement created event, for additional requests`(type: RequestForPlacementType, expectedTypeDescription: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementCreatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementCreatedFactory()
          .withRequestForPlacementType(type)
          .withExpectedArrival(LocalDate.of(2025, 3, 12))
          .withDuration(8)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A placement was requested with the reason '$expectedTypeDescription'. " +
        "The placement request is for Wednesday 12 March 2025 to Thursday 20 March 2025 (1 week and 1 day)",
    )
  }

  @Test
  fun `Returns expected description for request for placement created event, for initial request`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementCreatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementCreatedFactory()
          .withRequestForPlacementType(RequestForPlacementType.initial)
          .withExpectedArrival(LocalDate.of(2025, 3, 12))
          .withDuration(16)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A placement was automatically requested after the application was assessed. " +
        "The placement request is for Wednesday 12 March 2025 to Friday 28 March 2025 (2 weeks and 2 days)",
    )
  }

  @ParameterizedTest
  @EnumSource(value = RequestForPlacementAssessed.Decision::class)
  fun `Returns expected description for request for placement assessed event with summary`(decision: RequestForPlacementAssessed.Decision) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
    val expectedTermUsed = if (decision == RequestForPlacementAssessed.Decision.rejected) "was" else "is"

    every { mockDomainEventService.getRequestForPlacementAssessedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementAssessedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementAssessedFactory()
          .withDecision(decision)
          .withDecisionSummary("Request was $decision")
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement assessment was $decision. The placement request $expectedTermUsed for Friday 3 May 2024 to Friday 10 May 2024 (1 week). The reason was: Request was $decision.",
    )
  }

  @ParameterizedTest
  @EnumSource(value = RequestForPlacementAssessed.Decision::class)
  fun `Returns expected description for request for placement assessed event without summary`(decision: RequestForPlacementAssessed.Decision) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
    val expectedTermUsed = if (decision == RequestForPlacementAssessed.Decision.rejected) "was" else "is"

    every { mockDomainEventService.getRequestForPlacementAssessedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      RequestForPlacementAssessedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementAssessedFactory()
          .withDecision(decision)
          .withDecisionSummary(null)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      "A request for placement assessment was $decision. The placement request $expectedTermUsed for Friday 3 May 2024 to Friday 10 May 2024 (1 week).",
    )
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      ",,A request for placement was automatically allocated to an unknown user for assessment",
      "Alan,,A request for placement was automatically allocated to Alan A for assessment",
      ",Brenda,A request for placement was allocated to an unknown user by Brenda B for assessment",
      "Carol,Derek,A request for placement was allocated to Carol C by Derek D for assessment",
    ],
  )
  fun `Returns expected description for placement application allocated event`(
    allocatedTo: String?,
    allocatedBy: String?,
    expectedAllocationDescription: String,
  ) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED)

    every { mockDomainEventService.getPlacementApplicationAllocatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      PlacementApplicationAllocatedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.placementApplicationAllocated,
        eventDetails = PlacementApplicationAllocatedFactory()
          .withPlacementDates(
            listOf(
              DatePeriod(
                LocalDate.of(2025, 3, 12),
                LocalDate.of(2025, 3, 20),
              ),
            ),
          )
          .withAllocatedTo(
            allocatedTo?.let { allocatedTo ->
              StaffMemberFactory()
                .withForenames(allocatedTo)
                .withSurname(allocatedTo.first().toString())
                .produce()
            },
          )
          .withAllocatedBy(
            allocatedBy?.let { allocatedBy ->
              StaffMemberFactory()
                .withForenames(allocatedBy)
                .withSurname(allocatedBy.first().toString())
                .produce()
            },
          )
          .produce(),
      )
    }

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo("$expectedAllocationDescription. The placement request is for Wednesday 12 March 2025 to Thursday 20 March 2025 (1 week and 1 day)")
  }

  @Test
  fun `Returns a description for an info request event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED)
    val requestId = UUID.randomUUID()

    every { mockDomainEventService.getFurtherInformationRequestMadeEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      FurtherInformationRequestedEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.informationRequestMade,
        eventDetails = FurtherInformationRequestedFactory()
          .withRequestId(requestId)
          .produce(),
      )
    }

    val query = "Query goes here"
    val assessmentClarificationNote = AssessmentClarificationNoteEntityFactory()
      .withId(requestId)
      .withAssessment(mockk<AssessmentEntity>())
      .withCreatedBy(mockk<UserEntity>())
      .withQuery(query)
      .produce()

    every { mockAssessmentClarificationNoteRepository.findByIdOrNull(requestId) } returns assessmentClarificationNote

    val result = domainEventDescriber.getDescription(domainEventSummary)

    assertThat(result).isEqualTo(
      """
      A further information request was made to the applicant:
      "$query"
      """.trimIndent(),
    )
  }

  private fun <T> buildDomainEvent(builder: (UUID) -> T): DomainEvent<T> {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()

    return DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = "SOME-CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = builder(id),
    )
  }
}

data class DomainEventSummaryImpl(
  override val id: String,
  override val type: DomainEventType,
  override val occurredAt: OffsetDateTime,
  override val applicationId: UUID?,
  override val assessmentId: UUID?,
  override val bookingId: UUID?,
  override val premisesId: UUID?,
  override val appealId: UUID?,
  override val triggerSource: TriggerSourceType?,
  override val triggeredByUser: UserEntity?,
) : DomainEventSummary {
  companion object {
    fun ofType(type: DomainEventType) = DomainEventSummaryImpl(
      id = UUID.randomUUID().toString(),
      type = type,
      occurredAt = OffsetDateTime.now(),
      applicationId = null,
      assessmentId = null,
      bookingId = null,
      premisesId = null,
      appealId = null,
      triggerSource = null,
      triggeredByUser = null,
    )
  }
}
