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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
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
    matchRequestWithdrawnDetailUrlTemplate = UrlTemplate("http://api/events/match-request-withdrawn/#eventId"),
    assessmentAllocatedUrlTemplate = UrlTemplate("http://api/events/assessment-allocated/#eventId"),
    requestForPlacementCreatedUrlTemplate = UrlTemplate("http://api/events/request-for-placement-created/#eventId"),
  )

  @BeforeEach
  fun setupUserService() {
    every { userService.getUserForRequestOrNull() } returns user
  }

  @Test
  fun `getApplicationSubmittedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationSubmittedDomainEvent(id)).isNull()
  }

  @Test
  fun `getApplicationSubmittedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationSubmittedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.submitted",
      eventDetails = ApplicationSubmittedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationSubmittedDomainEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.application.submitted",
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
          it.eventType == "approved-premises.application.submitted" &&
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
        eventType = "approved-premises.application.submitted",
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
  fun `getApplicationAssessedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationAssessedDomainEvent(id)).isNull()
  }

  @Test
  fun `getApplicationAssessedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationAssessedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.assessed",
      eventDetails = ApplicationAssessedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationAssessedDomainEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.application.assessed",
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
          it.eventType == "approved-premises.application.assessed" &&
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
        eventType = "approved-premises.application.assessed",
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
  fun `getBookingMadeDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingMadeEvent(id)).isNull()
  }

  @Test
  fun `getBookingMadeDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingMadeEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.made",
      eventDetails = BookingMadeFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.booking.made",
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
          it.eventType == "approved-premises.booking.made" &&
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
        eventType = "approved-premises.booking.made",
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
  fun `getBookingChangedDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingChangedEvent(id)).isNull()
  }

  @Test
  fun `getBookingChangedDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingChangedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.changed",
      eventDetails = BookingChangedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingChangedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.booking.changed",
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
          it.eventType == "approved-premises.booking.changed" &&
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
        eventType = "approved-premises.booking.changed",
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
  fun `getBookingCancelledDomainEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingCancelledEvent(id)).isNull()
  }

  @Test
  fun `getBookingCancelledDomainEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingCancelledEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.cancelled",
      eventDetails = BookingCancelledFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingCancelledEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.booking.cancelled",
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
          it.eventType == "approved-premises.booking.cancelled" &&
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
        eventType = "approved-premises.booking.cancelled",
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
  fun `getPersonArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonArrivedEvent(id)).isNull()
  }

  @Test
  fun `getPersonArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonArrivedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.arrived",
      eventDetails = PersonArrivedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.person.arrived",
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
          it.eventType == "approved-premises.person.arrived" &&
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
        eventType = "approved-premises.person.arrived",
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
        eventType = "approved-premises.person.arrived",
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
  fun `getPersonNotArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonNotArrivedEvent(id)).isNull()
  }

  @Test
  fun `getPersonNotArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonNotArrivedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.not-arrived",
      eventDetails = PersonNotArrivedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonNotArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getPlacementApplicationWithdrawnEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPlacementApplicationWithdrawnEvent(id)).isNull()
  }

  @Test
  fun `getMatchRequestWithdrawnEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = MatchRequestWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.match-request.withdrawn",
      eventDetails = MatchRequestWithdrawnFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getMatchRequestWithdrawnEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getMatchRequestWithdrawnEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getMatchRequestWithdrawnEvent(id)).isNull()
  }

  @Test
  fun `getPlacementApplicationNotArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PlacementApplicationWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.placement-application.withdrawn",
      eventDetails = PlacementApplicationWithdrawnFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPlacementApplicationWithdrawnEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getRequestForPlacementCreated returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getRequestForPlacementCreatedEvent(id)).isNull()
  }

  @Test
  fun `getRequestForPlacementCreated returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = RequestForPlacementCreatedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.request-for-placement.created",
      eventDetails = RequestForPlacementCreatedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getRequestForPlacementCreatedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.person.not-arrived",
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
          it.eventType == "approved-premises.person.not-arrived" &&
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
        eventType = "approved-premises.person.not-arrived",
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
        eventType = "approved-premises.person.not-arrived",
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
  fun `getPersonDepartedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonDepartedEvent(id)).isNull()
  }

  @Test
  fun `getPersonDepartedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = PersonDepartedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.person.departed",
      eventDetails = PersonDepartedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonDepartedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.person.departed",
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
          it.eventType == "approved-premises.person.departed" &&
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
        eventType = "approved-premises.person.departed",
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
        eventType = "approved-premises.person.departed",
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
  fun `getBookingNotMadeEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingMadeEvent(id)).isNull()
  }

  @Test
  fun `getBookingNotMadeEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = BookingNotMadeEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.booking.not-made",
      eventDetails = BookingNotMadeFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingNotMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.booking.not-made",
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
          it.eventType == "approved-premises.booking.not-made" &&
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
        eventType = "approved-premises.booking.not-made",
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
  fun `getApplicationWithdrawnEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getApplicationWithdrawnEvent(id)).isNull()
  }

  @Test
  fun `getApplicationWithdrawnEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = ApplicationWithdrawnEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.application.withdrawn",
      eventDetails = ApplicationWithdrawnFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getApplicationWithdrawnEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.application.withdrawn",
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
          it.eventType == "approved-premises.application.withdrawn" &&
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
        eventType = "approved-premises.application.withdrawn",
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
        eventType = "approved-premises.application.withdrawn",
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
  fun `getAssessmentAppealedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getAssessmentAppealedEvent(id)).isNull()
  }

  @Test
  fun `getAssessmentAppealedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = AssessmentAppealedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.assessment.appealed",
      eventDetails = AssessmentAppealedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getAssessmentAppealedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.assessment.appealed",
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
          it.eventType == "approved-premises.assessment.appealed" &&
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
        eventType = "approved-premises.assessment.appealed",
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
        eventType = "approved-premises.placement-application.withdrawn",
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
          snsEvent.eventType == "approved-premises.placement-application.withdrawn" &&
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
        eventType = "approved-premises.placement-application.withdrawn",
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
        eventType = "approved-premises.match-request.withdrawn",
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
          snsEvent.eventType == "approved-premises.match-request.withdrawn" &&
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
        eventType = "approved-premises.match-request.withdrawn",
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
  fun `getAssessmentAllocatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getAssessmentAllocatedEvent(id)).isNull()
  }

  @Test
  fun `getAssessmentAllocatedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = AssessmentAllocatedEnvelope(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = "approved-premises.assessment.appealed",
      eventDetails = AssessmentAllocatedFactory().produce(),
    )

    every { domainEventRespositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getAssessmentAllocatedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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
        eventType = "approved-premises.assessment.allocated",
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
          it.eventType == "approved-premises.assessment.allocated" &&
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
        eventType = "approved-premises.assessment.allocated",
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
        eventType = "approved-premises.request-for-placement.created",
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
          snsEvent.eventType == "approved-premises.request-for-placement.created" &&
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
        eventType = "approved-premises.placement-request-created-request.withdrawn",
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
}
