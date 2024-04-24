package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.createDomainEventOfType
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
  private val mockDomainEventUrlConfig = mockk<DomainEventUrlConfig>()

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRespositoryMock,
    domainEventWorker = domainEventWorkerMock,
    userService = userService,
    emitDomainEventsEnabled = true,
    mockDomainEventUrlConfig,
  )

  private val detailUrl = "http://example.com/1234"

  @BeforeEach
  fun setupUserService() {
    every { userService.getUserForRequestOrNull() } returns user
    every { mockDomainEventUrlConfig.getUrlForDomainEventId(any(), any()) } returns detailUrl
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

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `saveAndEmit persists event and emits event to SNS`(domainEventType: DomainEventType) {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val crn = "CRN"
    val occurredAt = Instant.now()
    val data = createDomainEventOfType(domainEventType)
    val nomsNumber = "123"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = occurredAt,
      data = data,
    )

    every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

    domainEventService.saveAndEmit(domainEventToSave, domainEventType, crn, nomsNumber, true)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == domainEventType &&
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
          it.eventType == domainEventType.typeName &&
            it.version == 1 &&
            it.description == domainEventType.typeDescription &&
            it.detailUrl == detailUrl &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.additionalInformation.applicationId == applicationId &&
            it.personReference.identifiers.any { it.type == "CRN" && it.value == crn } &&
            it.personReference.identifiers.any { it.type == "NOMS" && it.value == nomsNumber }
        },
        domainEventToSave.id,
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(domainEventType, domainEventToSave.id)
    }
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `saveAndEmit persists event and does not emit event if emit is false`(domainEventType: DomainEventType) {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val crn = "CRN"
    val occurredAt = Instant.now()
    val data = createDomainEventOfType(domainEventType)
    val nomsNumber = "123"

    every { domainEventRespositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = occurredAt,
      data = data,
      bookingId = bookingId,
    )

    domainEventService.saveAndEmit(domainEventToSave, domainEventType, crn, nomsNumber, false)

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == domainEventType &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.bookingId == bookingId
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `saveAndEmit does not emit event to SNS if event fails to persist to database`(domainEventType: DomainEventType) {
    val id = UUID.randomUUID()
    val applicationId = UUID.randomUUID()
    val bookingId = UUID.randomUUID()
    val crn = "CRN"
    val occurredAt = Instant.now()
    val data = createDomainEventOfType(domainEventType)
    val nomsNumber = "123"

    every { domainEventRespositoryMock.save(any()) } throws RuntimeException("A database exception")

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = occurredAt,
      data = data,
      bookingId = bookingId,
    )

    try {
      domainEventService.saveAndEmit(domainEventToSave, domainEventType, crn, nomsNumber, true)
    } catch (_: Exception) {
    }

    verify(exactly = 1) {
      domainEventRespositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == domainEventType &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.bookingId == bookingId
        },
      )
    }

    verify(exactly = 0) {
      domainEventWorkerMock.emitEvent(any(), any())
    }
  }

  @Test
  fun `saveApplicationSubmittedDomainEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = ApplicationSubmittedFactory().produce()
    val domainEventEnvelope = mockk<ApplicationSubmittedEnvelope>()
    val domainEvent = mockk<DomainEvent<ApplicationSubmittedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveApplicationSubmittedDomainEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `saveApplicationAssessedDomainEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = ApplicationAssessedFactory().produce()
    val domainEventEnvelope = mockk<ApplicationAssessedEnvelope>()
    val domainEvent = mockk<DomainEvent<ApplicationAssessedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveApplicationAssessedDomainEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `saveBookingMadeDomainEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = BookingMadeFactory().produce()
    val domainEventEnvelope = mockk<BookingMadeEnvelope>()
    val domainEvent = mockk<DomainEvent<BookingMadeEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveBookingMadeDomainEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `savePersonArrivedEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = PersonArrivedFactory().produce()
    val domainEventEnvelope = mockk<PersonArrivedEnvelope>()
    val domainEvent = mockk<DomainEvent<PersonArrivedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), emit) } returns Unit

    domainEventServiceSpy.savePersonArrivedEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `savePersonNotArrivedEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = PersonNotArrivedFactory().produce()
    val domainEventEnvelope = mockk<PersonNotArrivedEnvelope>()
    val domainEvent = mockk<DomainEvent<PersonNotArrivedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), emit) } returns Unit

    domainEventServiceSpy.savePersonNotArrivedEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `savePersonDepartedEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = PersonDepartedFactory().produce()
    val domainEventEnvelope = mockk<PersonDepartedEnvelope>()
    val domainEvent = mockk<DomainEvent<PersonDepartedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), emit) } returns Unit

    domainEventServiceSpy.savePersonDepartedEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit,
      )
    }
  }

  @Test
  fun `saveBookingNotMadeEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = BookingNotMadeFactory().produce()
    val domainEventEnvelope = mockk<BookingNotMadeEnvelope>()
    val domainEvent = mockk<DomainEvent<BookingNotMadeEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveBookingNotMadeEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `saveBookingCancelledEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()
    val bookingId = UUID.randomUUID()

    val eventDetails = BookingCancelledFactory().produce()
    val domainEventEnvelope = mockk<BookingCancelledEnvelope>()
    val domainEvent = mockk<DomainEvent<BookingCancelledEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveBookingCancelledEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `saveBookingChangedEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = BookingChangedFactory().produce()
    val domainEventEnvelope = mockk<BookingChangedEnvelope>()
    val domainEvent = mockk<DomainEvent<BookingChangedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveBookingChangedEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `saveApplicationWithdrawnEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = ApplicationWithdrawnFactory().produce()
    val domainEventEnvelope = mockk<ApplicationWithdrawnEnvelope>()
    val domainEvent = mockk<DomainEvent<ApplicationWithdrawnEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), emit) } returns Unit

    domainEventServiceSpy.saveApplicationWithdrawnEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit,
      )
    }
  }

  @Test
  fun `saveAssessmentAppealedEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = AssessmentAppealedFactory().produce()
    val domainEventEnvelope = mockk<AssessmentAppealedEnvelope>()
    val domainEvent = mockk<DomainEvent<AssessmentAppealedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveAssessmentAppealedEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `savePlacementApplicationWithdrawnEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = PlacementApplicationWithdrawnFactory().produce()
    val domainEventEnvelope = mockk<PlacementApplicationWithdrawnEnvelope>()
    val domainEvent = mockk<DomainEvent<PlacementApplicationWithdrawnEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.savePlacementApplicationWithdrawnEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `savePlacementApplicationAllocatedEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = PlacementApplicationAllocatedFactory().produce()
    val domainEventEnvelope = mockk<PlacementApplicationAllocatedEnvelope>()
    val domainEvent = mockk<DomainEvent<PlacementApplicationAllocatedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.savePlacementApplicationAllocatedEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @Test
  fun `saveMatchRequestWithdrawnEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = MatchRequestWithdrawnFactory().produce()
    val domainEventEnvelope = mockk<MatchRequestWithdrawnEnvelope>()
    val domainEvent = mockk<DomainEvent<MatchRequestWithdrawnEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveMatchRequestWithdrawnEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `saveRequestForPlacementCreatedEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = RequestForPlacementCreatedFactory().produce()
    val domainEventEnvelope = mockk<RequestForPlacementCreatedEnvelope>()
    val domainEvent = mockk<DomainEvent<RequestForPlacementCreatedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), emit) } returns Unit

    domainEventServiceSpy.saveRequestForPlacementCreatedEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit,
      )
    }
  }

  @Test
  fun `saveAssessmentAllocatedEvent sends correct arguments to saveAndEmit`() {
    val id = UUID.randomUUID()

    val eventDetails = AssessmentAllocatedFactory().produce()
    val domainEventEnvelope = mockk<AssessmentAllocatedEnvelope>()
    val domainEvent = mockk<DomainEvent<AssessmentAllocatedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any()) } returns Unit

    domainEventServiceSpy.saveAssessmentAllocatedEvent(domainEvent)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
      )
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `saveFurtherInformationRequestedEvent sends correct arguments to saveAndEmit`(emit: Boolean) {
    val id = UUID.randomUUID()

    val eventDetails = FurtherInformationRequestedFactory().produce()
    val domainEventEnvelope = mockk<FurtherInformationRequestedEnvelope>()
    val domainEvent = mockk<DomainEvent<FurtherInformationRequestedEnvelope>>()

    every { domainEvent.id } returns id
    every { domainEvent.data } returns domainEventEnvelope
    every { domainEventEnvelope.eventDetails } returns eventDetails

    val domainEventServiceSpy = spyk(domainEventService)

    every { domainEventServiceSpy.saveAndEmit(any(), any(), any(), any(), null, emit) } returns Unit

    domainEventServiceSpy.saveFurtherInformationRequestedEvent(domainEvent, emit)

    verify {
      domainEventServiceSpy.saveAndEmit(
        domainEvent = domainEvent,
        eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
        crn = eventDetails.personReference.crn,
        nomsNumber = eventDetails.personReference.noms,
        emit = emit,
      )
    }
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
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED to domainEventService::getFurtherInformationRequestMadeEvent,
    )[type]!!
  }
}
