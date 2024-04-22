package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("CyclomaticComplexMethod")
class DomainEventServiceTest {
  private val domainEventRespositoryMock = mockk<DomainEventRepository>()
  private val domainEventWorkerMock = mockk<ConfiguredDomainEventWorker>()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }
  private val userService = mockk<UserService>()
  private val user = UserEntityFactory().withDefaultProbationRegion().produce()

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRespositoryMock,
    domainEventWorker = domainEventWorkerMock,
    userService = userService,
    emitDomainEventsEnabled = true,
    applicationSubmittedDetailUrlTemplate = "http://api/events/application-submitted/#eventId",
    applicationAssessedDetailUrlTemplate = "http://api/events/application-assessed/#eventId",
    bookingMadeDetailUrlTemplate = "http://api/events/booking-made/#eventId",
    personArrivedDetailUrlTemplate = "http://api/events/person-arrived/#eventId",
    personNotArrivedDetailUrlTemplate = "http://api/events/person-not-arrived/#eventId",
    personDepartedDetailUrlTemplate = "http://api/events/person-departed/#eventId",
    bookingNotMadeDetailUrlTemplate = "http://api/events/booking-not-made/#eventId",
    bookingCancelledDetailUrlTemplate = "http://api/events/booking-cancelled/#eventId",
    bookingChangedDetailUrlTemplate = "http://api/events/booking-changed/#eventId",
    applicationWithdrawnDetailUrlTemplate = "http://api/events/application-withdrawn/#eventId",
    assessmentAppealedDetailUrlTemplate = "http://api/events/assessment-appealed/#eventId",
    placementApplicationWithdrawnDetailUrlTemplate = UrlTemplate("http://api/events/placement-application-withdrawn/#eventId"),
    placementApplicationAllocatedDetailUrlTemplate = UrlTemplate("http://api/events/placement-application-allocated/#eventId"),
    matchRequestWithdrawnDetailUrlTemplate = UrlTemplate("http://api/events/match-request-withdrawn/#eventId"),
    assessmentAllocatedUrlTemplate = UrlTemplate("http://api/events/assessment-allocated/#eventId"),
    requestForPlacementCreatedUrlTemplate = UrlTemplate("http://api/events/request-for-placement-created/#eventId"),
  )

  @BeforeEach
  fun setupUserService() {
    every { userService.getUserForRequestOrNull() } returns user
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `getDomainEvent returns null when event not found`(domainEventType: DomainEventType) {
    val id = UUID.randomUUID()
    val method = fetchGetterForType(domainEventType)

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(method.invoke(id)).isNull()
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `getDomainEvent returns event`(domainEventType: DomainEventType) {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()
    val occurredAt = OffsetDateTime.now()
    val crn = "CRN"

    val method = fetchGetterForType(domainEventType)
    val data = createDomainEventOfType(domainEventType)

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(domainEventType)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = method.invoke(id)

    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  private fun fetchGetterForType(type: DomainEventType): (UUID) -> DomainEvent<out Any>? {
    return mapOf(
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED to domainEventService::getApplicationSubmittedDomainEvent,
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED to domainEventService::getApplicationAssessedDomainEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE to domainEventService::getBookingMadeEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED to domainEventService::getPersonArrivedEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED to domainEventService::getPersonNotArrivedEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED to domainEventService::getPersonDepartedEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE to domainEventService::getBookingNotMadeEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED to domainEventService::getBookingCancelledEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED to domainEventService::getBookingChangedEvent,
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN to domainEventService::getApplicationWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED to domainEventService::getAssessmentAppealedEvent,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED to domainEventService::getAssessmentAllocatedEvent,
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN to domainEventService::getPlacementApplicationWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED to domainEventService::getPlacementApplicationAllocatedEvent,
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN to domainEventService::getMatchRequestWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED to domainEventService::getRequestForPlacementCreatedEvent,
    )[type]!!
  }

  private fun createDomainEventOfType(type: DomainEventType): Any {
    val id = UUID.randomUUID()
    val timestamp = Instant.now()
    val eventType = EventType.entries.find { it.value == type.typeName } ?: throw RuntimeException("Cannot find EventType for $type")

    return when (type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> ApplicationSubmittedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = ApplicationSubmittedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> ApplicationAssessedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = ApplicationAssessedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> BookingMadeEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = BookingMadeFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> PersonArrivedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = PersonArrivedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> PersonNotArrivedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = PersonNotArrivedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> PersonDepartedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = PersonDepartedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> BookingNotMadeEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = BookingNotMadeFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> BookingCancelledEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = BookingCancelledFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> BookingChangedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = BookingChangedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = ApplicationWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> AssessmentAppealedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = AssessmentAppealedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> AssessmentAllocatedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = AssessmentAllocatedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawnEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = PlacementApplicationWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> PlacementApplicationAllocatedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = PlacementApplicationAllocatedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> MatchRequestWithdrawnEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = MatchRequestWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> RequestForPlacementCreatedEnvelope(
        id = id,
        timestamp = timestamp,
        eventType = eventType,
        eventDetails = RequestForPlacementCreatedFactory().produce(),
      )
      else -> throw RuntimeException("Domain even type $type not supported")
    }
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationSubmitted,
        eventDetails = ApplicationSubmittedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.applicationSubmitted.value &&
            it.version == 1 &&
            it.description == "An application has been submitted for an Approved Premises placement" &&
            it.detailUrl == "http://api/events/application-submitted/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationSubmitted,
        eventDetails = ApplicationSubmittedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationSubmittedDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveApplicationAssessedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationAssessed,
        eventDetails = ApplicationAssessedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.applicationAssessed.value &&
            it.version == 1 &&
            it.description == "An application has been assessed for an Approved Premises placement" &&
            it.detailUrl == "http://api/events/application-assessed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationAssessedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationAssessed,
        eventDetails = ApplicationAssessedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationAssessedDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveBookingMadeDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingMade,
        eventDetails = BookingMadeFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingMadeDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.bookingMade.value &&
            it.version == 1 &&
            it.description == "An Approved Premises booking has been made" &&
            it.detailUrl == "http://api/events/booking-made/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingMadeDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingMade,
        eventDetails = BookingMadeFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingMadeDomainEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveBookingChangedDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val bookingId = UUID.fromString("b831ead2-31ae-4907-8e1c-cad74cb9667c")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      bookingId = bookingId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingChangedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingChanged,
        eventDetails = BookingChangedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingChangedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.bookingId == bookingId
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.bookingChanged.value &&
            it.version == 1 &&
            it.description == "An Approved Premises Booking has been changed" &&
            it.detailUrl == "http://api/events/booking-changed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingChangedDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingChangedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingChanged,
        eventDetails = BookingChangedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingChangedEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveBookingCancelledDomainEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingCancelledEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingCancelled,
        eventDetails = BookingCancelledFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingCancelledEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.bookingCancelled.value &&
            it.version == 1 &&
            it.description == "An Approved Premises Booking has been cancelled" &&
            it.detailUrl == "http://api/events/booking-cancelled/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingCancelledDomainEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingCancelledEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingCancelled,
        eventDetails = BookingCancelledFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingCancelledEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `savePersonArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonArrivedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.personArrived.value &&
            it.version == 1 &&
            it.description == "Someone has arrived at an Approved Premises for their Booking" &&
            it.detailUrl == "http://api/events/person-arrived/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonArrivedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonArrivedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonArrivedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = PersonArrivedFactory().produce(),
      ),
    )

    domainEventService.savePersonArrivedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonNotArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personNotArrived,
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.personNotArrived.value &&
            it.version == 1 &&
            it.description == "Someone has failed to arrive at an Approved Premises for their Booking" &&
            it.detailUrl == "http://api/events/person-not-arrived/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonNotArrivedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personNotArrived,
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonNotArrivedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personNotArrived,
        eventDetails = PersonNotArrivedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonNotArrivedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonDepartedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonDepartedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.personDeparted.value &&
            it.version == 1 &&
            it.description == "Someone has left an Approved Premises" &&
            it.detailUrl == "http://api/events/person-departed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePersonDepartedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePersonDepartedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `savePersonDepartedEvent persists event, does not emit event to SNS if emit flag is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = PersonDepartedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePersonDepartedEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.bookingId == domainEventToSave.bookingId &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify { domainEventWorkerMock wasNot Called }
  }

  @Test
  fun `saveBookingNotMadeEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingNotMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingNotMade,
        eventDetails = BookingNotMadeFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveBookingNotMadeEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.bookingNotMade.value &&
            it.version == 1 &&
            it.description == "It was not possible to create a Booking on this attempt" &&
            it.detailUrl == "http://api/events/booking-not-made/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveBookingNotMadeEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = BookingNotMadeEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingNotMade,
        eventDetails = BookingNotMadeFactory().produce(),
      ),
    )

    try {
      domainEventService.saveBookingNotMadeEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveApplicationWithdrawnEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveApplicationWithdrawnEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.applicationWithdrawn.value &&
            it.version == 1 &&
            it.description == "An Approved Premises Application has been withdrawn" &&
            it.detailUrl == "http://api/events/application-withdrawn/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveApplicationWithdrawnEvent does not emit event to SNS if emit parameter is false`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory().produce(),
      ),
    )

    domainEventService.saveApplicationWithdrawnEvent(domainEventToSave, emit = false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) { domainEventWorkerMock.emitEvent(any(), any()) }
  }

  @Test
  fun `saveApplicationWithdrawnEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationWithdrawn,
        eventDetails = ApplicationWithdrawnFactory().produce(),
      ),
    )

    try {
      domainEventService.saveApplicationWithdrawnEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveAssessmentAppealedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = AssessmentAppealedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.assessmentAppealed,
        eventDetails = AssessmentAppealedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveAssessmentAppealedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.assessmentAppealed.value &&
            it.version == 1 &&
            it.description == "An Approved Premises Assessment has been appealed" &&
            it.detailUrl == "http://api/events/assessment-appealed/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveAssessmentAppealedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = AssessmentAppealedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.assessmentAppealed,
        eventDetails = AssessmentAppealedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveAssessmentAppealedEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `savePlacementApplicationWithdrawnEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PlacementApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.placementApplicationWithdrawn,
        eventDetails = PlacementApplicationWithdrawnFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePlacementApplicationWithdrawnEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match { snsEvent ->
          snsEvent.eventType == EventType.placementApplicationWithdrawn.value &&
            snsEvent.version == 1 &&
            snsEvent.description == "An Approved Premises Request for Placement has been withdrawn" &&
            snsEvent.detailUrl == "http://api/events/placement-application-withdrawn/$id" &&
            snsEvent.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            snsEvent.additionalInformation.applicationId == applicationId &&
            snsEvent.personReference.identifiers.any { id -> id.type == "CRN" && id.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            snsEvent.personReference.identifiers.any { id -> id.type == "NOMS" && id.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePlacementApplicationWithdrawnEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PlacementApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.placementApplicationWithdrawn,
        eventDetails = PlacementApplicationWithdrawnFactory().produce(),
      ),
    )

    try {
      domainEventService.savePlacementApplicationWithdrawnEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveMatchRequestWithdrawnEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = MatchRequestWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.matchRequestWithdrawn,
        eventDetails = MatchRequestWithdrawnFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveMatchRequestWithdrawnEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match { snsEvent ->
          snsEvent.eventType == EventType.matchRequestWithdrawn.value &&
            snsEvent.version == 1 &&
            snsEvent.description == "An Approved Premises Match Request has been withdrawn" &&
            snsEvent.detailUrl == "http://api/events/match-request-withdrawn/$id" &&
            snsEvent.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            snsEvent.additionalInformation.applicationId == applicationId &&
            snsEvent.personReference.identifiers.any { id -> id.type == "CRN" && id.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            snsEvent.personReference.identifiers.any { id -> id.type == "NOMS" && id.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveMatchRequestWithdrawnEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = MatchRequestWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.matchRequestWithdrawn,
        eventDetails = MatchRequestWithdrawnFactory().produce(),
      ),
    )

    try {
      domainEventService.saveMatchRequestWithdrawnEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveAssessmentAllocatedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = AssessmentAllocatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.assessmentAllocated,
        eventDetails = AssessmentAllocatedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveAssessmentAllocatedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == EventType.assessmentAllocated.value &&
            it.version == 1 &&
            it.description == "An Approved Premises Assessment has been allocated" &&
            it.detailUrl == "http://api/events/assessment-allocated/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveAssessmentAllocatedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = AssessmentAllocatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.assessmentAllocated,
        eventDetails = AssessmentAllocatedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveAssessmentAllocatedEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveRequestForPlacementCreated persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = RequestForPlacementCreatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementCreatedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveRequestForPlacementCreatedEvent(domainEventToSave, emit = true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match { snsEvent ->
          snsEvent.eventType == EventType.requestForPlacementCreated.value &&
            snsEvent.version == 1 &&
            snsEvent.description == "An Approved Premises Request for Placement has been created" &&
            snsEvent.detailUrl == "http://api/events/request-for-placement-created/$id" &&
            snsEvent.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            snsEvent.additionalInformation.applicationId == applicationId &&
            snsEvent.personReference.identifiers.any { id -> id.type == "CRN" && id.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            snsEvent.personReference.identifiers.any { id -> id.type == "NOMS" && id.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `saveRequestForPlacementCreated does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = RequestForPlacementCreatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.requestForPlacementCreated,
        eventDetails = RequestForPlacementCreatedFactory().produce(),
      ),
    )

    try {
      domainEventService.saveRequestForPlacementCreatedEvent(domainEventToSave, emit = true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `savePlacementApplicationAllocatedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PlacementApplicationAllocatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.assessmentAllocated,
        eventDetails = PlacementApplicationAllocatedFactory().produce(),
      ),
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.savePlacementApplicationAllocatedEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      domainEventWorkerMock.emitEvent(
        match {
          it.eventType == "approved-premises.placement-application.allocated" &&
            it.version == 1 &&
            it.description == "An Approved Premises Request for Placement has been allocated" &&
            it.detailUrl == "http://api/events/placement-application-allocated/$id" &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
        domainEventToSave.id,
      )
    }
  }

  @Test
  fun `savePlacementApplicationAllocatedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = PlacementApplicationAllocatedEnvelope(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.placementApplicationAllocated,
        eventDetails = PlacementApplicationAllocatedFactory().produce(),
      ),
    )

    try {
      domainEventService.savePlacementApplicationAllocatedEvent(domainEventToSave)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }
}
