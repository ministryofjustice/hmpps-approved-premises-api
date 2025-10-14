package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationExpiredManuallyPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingKeyWorkerAssignedFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventDescriber
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.TimelineFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toInstant
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiDateTimeFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MaxLineLength")
class Cas1DomainEventDescriberTest {
  private val mockDomainEventService = mockk<Cas1DomainEventService>()
  private val mockAssessmentClarificationNoteRepository = mockk<AssessmentClarificationNoteRepository>()
  private val payloadFactories = mutableListOf<TimelineFactory<*>>()

  private val cas1DomainEventDescriber = Cas1DomainEventDescriber(
    mockDomainEventService,
    mockAssessmentClarificationNoteRepository,
    payloadFactories,
    emptyList(),
  )

  @Test
  fun `Returns expected description for application submitted event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The application was submitted")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted", "rejected"])
  fun `Returns expected description for application assessed event`(decision: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)

    every { mockDomainEventService.getApplicationAssessedDomainEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.applicationAssessed,
        eventDetails = ApplicationAssessedFactory()
          .withDecision(decision)
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The application was assessed and $decision")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01T12:34:00", "2024-05-02T13:44:00", "2024-12-02T00:00:00"])
  fun `Returns expected description for person arrived event`(arrivalDate: LocalDateTime) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)

    every { mockDomainEventService.getPersonArrivedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personArrived,
        eventDetails = PersonArrivedFactory()
          .withArrivedAt(arrivalDate.toInstant())
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The person moved into Test premises on ${arrivalDate.toUiDateTimeFormat()}")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-01-01", "2024-01-02"])
  fun `Returns expected description for person not arrived event`(expectedArrivalDate: LocalDate) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)

    every { mockDomainEventService.getPersonNotArrivedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personNotArrived,
        eventDetails = PersonNotArrivedFactory()
          .withExpectedArrivalOn(expectedArrivalDate)
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The person was due to move into Test premises on ${expectedArrivalDate.toUiFormat()} but did not arrive")
  }

  @ParameterizedTest
  @CsvSource(value = ["2024-04-01T09:32:00", "2024-04-02T15:33:00", "2024-12-02T00:00:00"])
  fun `Returns expected description for person departed event`(departureDate: LocalDateTime) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)

    every { mockDomainEventService.getPersonDepartedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.personDeparted,
        eventDetails = PersonDepartedFactory()
          .withDepartedAt(departureDate.toInstant())
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The person moved out of Test premises on ${departureDate.toUiDateTimeFormat()}")
  }

  @ParameterizedTest
  @CsvSource(value = ["Reason A", "Reason B"])
  fun `Returns expected description for booking not made event`(reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingNotMade,
        eventDetails = BookingNotMadeFactory()
          .withFailureDescription(reason)
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("A placement was not made for the placement request. The reason was: $reason")
  }

  @Test
  fun `Returns expected description for booking not made event with no failure description`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
    val bookingNotMade = BookingNotMadeFactory().produce().copy(failureDescription = null)

    every { mockDomainEventService.getBookingNotMadeEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingNotMade,
        eventDetails = bookingNotMade,
      )
    }
    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("A placement was not made for the placement request.")
  }

  @Test
  fun `Returns expected description for booking keyworker assigned event without previous keyworker`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED)

    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { mockDomainEventService.getBookingKeyWorkerAssignedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingChanged,
        eventDetails = BookingKeyWorkerAssignedFactory()
          .withArrivalDate(arrivalDate)
          .withDepartureDate(departureDate)
          .withAssignedKeyWorkerName("assigned keyWorker")
          .withPremises(
            EventPremisesFactory()
              .withName("The Premises Name")
              .produce(),
          )
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("Keyworker for placement at The Premises Name for Monday 1 January 2024 to Monday 1 April 2024 set to assigned keyWorker")
  }

  @Test
  fun `Returns expected description for booking keyworker assigned event with previous keyworker`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED)

    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { mockDomainEventService.getBookingKeyWorkerAssignedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.bookingChanged,
        eventDetails = BookingKeyWorkerAssignedFactory()
          .withArrivalDate(arrivalDate)
          .withDepartureDate(departureDate)
          .withAssignedKeyWorkerName("assigned keyWorker")
          .withPreviousKeyWorkerName("previous keyWorker")
          .withPremises(
            EventPremisesFactory()
              .withName("The Premises Name")
              .produce(),
          )
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("Keyworker for placement at The Premises Name for Monday 1 January 2024 to Monday 1 April 2024 changes from previous keyWorker to assigned keyWorker")
  }

  @Test
  fun `Returns expected description for application withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("change_in_circumstances_new_application_to_be_submitted")
          .withOtherWithdrawalReason(null)
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The application was withdrawn. The reason was: 'change in circumstances new application to be submitted'")
  }

  @Test
  fun `Returns expected description and payload for application expired manually event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED_MANUALLY)

    val mockFactory = mockk<TimelineFactory<*>>()
    every { mockFactory.forType() } returns DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED_MANUALLY
    every { mockFactory.produce(any()) } returns Cas1ApplicationExpiredManuallyPayload(
      type = Cas1TimelineEventType.applicationManuallyExpired,
      expiredReason = "Superseded by another application.",
    )
    payloadFactories.add(mockFactory)

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)
    assertThat(result.description).isNull()

    val payload = result.payload as Cas1ApplicationExpiredManuallyPayload
    assertThat(payload.type).isEqualTo(Cas1TimelineEventType.applicationManuallyExpired)
    assertThat(payload.expiredReason).isEqualTo("Superseded by another application.")
  }

  @Test
  fun `Returns expected description for application withdrawn event with additional reason`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getApplicationWithdrawnEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory()
          .withWithdrawalReason("the main withdrawal reason")
          .withOtherWithdrawalReason("additional reason")
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The application was withdrawn. The reason was: 'the main withdrawal reason' (additional reason)")
  }

  @ParameterizedTest
  @CsvSource(value = ["accepted,Reason A", "rejected,Reason B"])
  fun `Returns expected description for assessment appealed event`(decision: String, reason: String) {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)

    every { mockDomainEventService.getAssessmentAppealedEvent(any()) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.assessmentAppealed,
        eventDetails = AssessmentAppealedFactory()
          .withDecision(AppealDecision.valueOf(decision))
          .withDecisionDetail(reason)
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("The assessment was appealed and $decision. The reason was: $reason")
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
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(expectedDescription)
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with no dates`() {
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.placementApplicationWithdrawn,
        eventDetails = PlacementApplicationWithdrawnFactory()
          .withWithdrawalReason(PlacementApplicationWithdrawalReason.RELATED_APPLICATION_WITHDRAWN.toString())
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("A request for placement was withdrawn. The reason was: 'Related application withdrawn'")
  }

  @Test
  fun `Returns expected description for placement application withdrawn event with placement dates`() {
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)

    every { mockDomainEventService.getPlacementApplicationWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
      "A request for placement was withdrawn for dates Tuesday 2 January 2024 to Monday 4 March 2024, Monday 6 May 2024 to Monday 8 July 2024. " +
        "The reason was: 'Duplicate placement request'",
    )
  }

  @Test
  fun `Returns expected description for match request withdrawn event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)

    every { mockDomainEventService.getMatchRequestWithdrawnEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
        id = it,
        timestamp = Instant.now(),
        eventType = EventType.matchRequestWithdrawn,
        eventDetails = MatchRequestWithdrawnFactory()
          .withWithdrawalReason(PlacementRequestWithdrawalReason.NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION.toString())
          .withDatePeriod(DatePeriod(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 3, 4)))
          .produce(),
      )
    }

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
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
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
      "A placement was requested with the reason '$expectedTypeDescription'. " +
        "The placement request is for Wednesday 12 March 2025 to Thursday 20 March 2025 (1 week and 1 day)",
    )
  }

  @Test
  fun `Returns expected description for request for placement created event, for initial request`() {
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)

    every { mockDomainEventService.getRequestForPlacementCreatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
      "A placement was automatically requested after the application was assessed. " +
        "The placement request is for Wednesday 12 March 2025 to Friday 28 March 2025 (2 weeks and 2 days)",
    )
  }

  @ParameterizedTest
  @EnumSource(value = RequestForPlacementAssessed.Decision::class)
  fun `Returns expected description for request for placement assessed event with summary`(decision: RequestForPlacementAssessed.Decision) {
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
    val expectedTermUsed = if (decision == RequestForPlacementAssessed.Decision.rejected) "was" else "is"

    every { mockDomainEventService.getRequestForPlacementAssessedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
      "A request for placement assessment was $decision. The placement request $expectedTermUsed for Friday 3 May 2024 to Friday 10 May 2024 (1 week). The reason was: Request was $decision.",
    )
  }

  @ParameterizedTest
  @EnumSource(value = RequestForPlacementAssessed.Decision::class)
  fun `Returns expected description for request for placement assessed event without summary`(decision: RequestForPlacementAssessed.Decision) {
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED)
    val expectedTermUsed = if (decision == RequestForPlacementAssessed.Decision.rejected) "was" else "is"

    every { mockDomainEventService.getRequestForPlacementAssessedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
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
    val domainEventSummary =
      DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED)

    every { mockDomainEventService.getPlacementApplicationAllocatedEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo("$expectedAllocationDescription. The placement request is for Wednesday 12 March 2025 to Thursday 20 March 2025 (1 week and 1 day)")
  }

  @Test
  fun `Returns a description for an info request event`() {
    val domainEventSummary = DomainEventSummaryImpl.ofType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED)
    val requestId = UUID.randomUUID()

    every { mockDomainEventService.getFurtherInformationRequestMadeEvent(UUID.fromString(domainEventSummary.id)) } returns buildDomainEvent {
      Cas1DomainEventEnvelope(
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

    val result = cas1DomainEventDescriber.getDescriptionAndPayload(domainEventSummary)

    assertThat(result.description).isEqualTo(
      """
      A further information request was made to the applicant:
      "$query"
      """.trimIndent(),
    )
  }

  private fun <T> buildDomainEvent(
    builder: (UUID) -> T,
  ): GetCas1DomainEvent<T> {
    val id = UUID.randomUUID()
    return GetCas1DomainEvent(
      id = id,
      data = builder(id),
      schemaVersion = null,
      spaceBookingId = null,
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
  override val cas1SpaceBookingId: UUID?,
  override val triggerSource: TriggerSourceType?,
  override val triggeredByUser: UserEntity?,
  override val schemaVersion: Int? = null,
  override val premisesName: String?,
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
      cas1SpaceBookingId = null,
      triggerSource = null,
      triggeredByUser = null,
      premisesName = "Test premises",
    )
  }
}
