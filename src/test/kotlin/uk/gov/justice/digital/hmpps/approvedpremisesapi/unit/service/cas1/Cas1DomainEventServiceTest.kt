package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingKeyWorkerAssignedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventMigrationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEventWithPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas1DomainEventsFactory
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("CyclomaticComplexMethod", "LargeClass")
class Cas1DomainEventServiceTest {
  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val domainEventWorkerMock = mockk<ConfiguredDomainEventWorker>()
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()
  private val userService = mockk<UserService>()
  private val user = UserEntityFactory().withDefaultProbationRegion().produce()
  private val mockDomainEventUrlConfig = mockk<DomainEventUrlConfig>()
  private val sentryService = mockk<SentryService>()
  private val domainEventsFactory = Cas1DomainEventsFactory(objectMapper)

  private val domainEventService = buildService(emitDomainEventsEnabled = true)
  private val domainEventServiceEmitDisabled = buildService(emitDomainEventsEnabled = false)

  private fun buildService(emitDomainEventsEnabled: Boolean) = Cas1DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRepositoryMock,
    domainEventWorker = domainEventWorkerMock,
    userService = userService,
    emitDomainEventsEnabled = emitDomainEventsEnabled,
    mockDomainEventUrlConfig,
    Cas1DomainEventMigrationService(objectMapper, userService),
    sentryService,
  )

  private val detailUrl = "http://example.com/1234"

  companion object {
    @JvmStatic
    fun allCas1DomainEventTypes() = DomainEventType
      .entries
      .filter { it.cas == DomainEventCas.CAS1 && it.cas1Info!!.emittable }

    @JvmStatic
    fun allEmittableCas1DomainEventTypes() = DomainEventType
      .entries
      .filter { it.cas == DomainEventCas.CAS1 && it.cas1Info!!.emittable }

    @JvmStatic
    fun allNonEmittableCas1DomainEventTypes() = DomainEventType
      .entries
      .filter { it.cas == DomainEventCas.CAS1 && !it.cas1Info!!.emittable }
  }

  @BeforeEach
  fun setupUserService() {
    every { userService.getUserForRequestOrNull() } returns user
    every { mockDomainEventUrlConfig.getUrlForDomainEventId(any(), any()) } returns detailUrl
  }

  @Nested
  inner class GetDomainEvents {

    @Test
    fun `getDomainEvent returns error for payload mismatch`() {
      val id = UUID.fromString("0adab8a6-14a6-4c41-a56e-7f0bb76d3d02")

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
        .withId(id)
        .withType(DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED)
        .withData(domainEventsFactory.createEnvelopeLatestVersion(DomainEventType.APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED).persistedJson)
        .produce()

      assertThatThrownBy(
        {
          domainEventService.getApplicationAssessedDomainEvent(id)
        },
      ).hasMessage(
        "Entity with id 0adab8a6-14a6-4c41-a56e-7f0bb76d3d02 has type APPROVED_PREMISES_PLACEMENT_CHANGE_REQUEST_REJECTED, " +
          "which contains data of type class uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementChangeRequestRejected. " +
          "This is incompatible with the requested payload type class " +
          "uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed.",
      )
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `getDomainEvent returns null when event not found`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val method = fetchGetterForType(type)

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

      assertThat(method.invoke(id)).isNull()
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `getDomainEvent returns event with deserialized json`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val occurredAt = OffsetDateTime.now()
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"

      val method = fetchGetterForType(type)
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(
        type = type,
        occurredAt = occurredAt.toInstant(),
      )

      val spaceBookingId = UUID.randomUUID()

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
        .withId(id)
        .withApplicationId(applicationId)
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .withType(type)
        .withData(domainEventAndJson.persistedJson)
        .withOccurredAt(occurredAt)
        .withSchemaVersion(domainEventAndJson.schemaVersion.versionNo)
        .withCas1SpaceBookingId(spaceBookingId)
        .produce()

      val event = method.invoke(id)

      assertThat(event).isEqualTo(
        GetCas1DomainEvent(
          id = id,
          data = domainEventAndJson.envelope,
          schemaVersion = domainEventAndJson.schemaVersion.versionNo,
          spaceBookingId = spaceBookingId,
        ),
      )
    }

    private fun fetchGetterForType(type: DomainEventType): (UUID) -> GetCas1DomainEvent<out Any>? = mapOf(
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED to domainEventService::getApplicationSubmittedDomainEvent,
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED to domainEventService::getApplicationAssessedDomainEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE to domainEventService::getBookingMadeEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED to domainEventService::getPersonArrivedEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED to domainEventService::getPersonNotArrivedEvent,
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED to domainEventService::getPersonDepartedEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE to domainEventService::getBookingNotMadeEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED to domainEventService::getBookingCancelledEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED to domainEventService::getBookingChangedEvent,
      DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED to domainEventService::getBookingKeyWorkerAssignedEvent,
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN to domainEventService::getApplicationWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED to domainEventService::getApplicationExpiredEvent,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED to domainEventService::getAssessmentAppealedEvent,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED to domainEventService::getAssessmentAllocatedEvent,
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN to domainEventService::getPlacementApplicationWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED to domainEventService::getPlacementApplicationAllocatedEvent,
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN to domainEventService::getMatchRequestWithdrawnEvent,
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED to domainEventService::getRequestForPlacementCreatedEvent,
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED to domainEventService::getRequestForPlacementAssessedEvent,
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED to domainEventService::getFurtherInformationRequestMadeEvent,
    )[type]!!
  }

  @Nested
  inner class Save {

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `persists event and emits event to SNS if emittable`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type, occurredAt = occurredAt, id = id)
      val metadata = mapOf(MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to "value1")

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEventWithPayload(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope.eventDetails,
        bookingId = bookingId,
        metadata = metadata,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
        triggerSource = TriggerSourceType.USER,
        type = type,
      )

      every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

      domainEventService.save(domainEventToSave)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
            assertThat(it.metadata).isEqualTo(metadata)
            assertThat(it.triggerSource).isEqualTo(TriggerSourceType.USER)
          },
        )
      }

      verify(exactly = 1) {
        domainEventWorkerMock.emitEvent(
          match {
            it.eventType == type.typeName &&
              it.version == 1 &&
              it.description == type.typeDescription &&
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
        mockDomainEventUrlConfig.getUrlForDomainEventId(type, domainEventToSave.id)
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `saveAndEmit persists event and does not emit event if emit on domain event is false`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val cas1SpaceBookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type, occurredAt = occurredAt, id = id)

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEventWithPayload(
        id = id,
        type = type,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope.eventDetails,
        bookingId = bookingId,
        cas1SpaceBookingId = cas1SpaceBookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
        emit = false,
      )

      domainEventService.save(domainEventToSave)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
            assertThat(it.cas1SpaceBookingId).isEqualTo(cas1SpaceBookingId)
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allNonEmittableCas1DomainEventTypes")
    fun `saveAndEmit does not emit event to SNS if event type not emittable`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type, occurredAt = occurredAt, id = id)

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEventWithPayload(
        id = id,
        type = type,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope.eventDetails,
        bookingId = bookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
      )

      domainEventService.save(domainEventToSave)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `saveAndEmit does not emit event to SNS if event fails to persist to database`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type, occurredAt = occurredAt)

      every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

      val domainEventToSave = SaveCas1DomainEventWithPayload(
        id = id,
        type = type,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope.eventDetails,
        bookingId = bookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
      )

      try {
        domainEventService.save(domainEventToSave)
      } catch (_: Exception) {
      }

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            it.id == domainEventToSave.id &&
              it.type == type &&
              it.crn == crn &&
              it.nomsNumber == nomsNumber &&
              it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
              it.data == domainEventAndJson.persistedJson &&
              it.triggeredByUserId == user.id &&
              it.bookingId == bookingId
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }
  }

  @Nested
  inner class SaveAndEmitForEnvelope {

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `saveAndEmit persists event and emits event to SNS if emittable`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type)
      val metadata = mapOf(MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to "value1")

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope,
        bookingId = bookingId,
        metadata = metadata,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
        triggerSource = TriggerSourceType.USER,
      )

      every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

      domainEventService.saveAndEmitForEnvelope(domainEventToSave, type)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
            assertThat(it.metadata).isEqualTo(metadata)
            assertThat(it.triggerSource).isEqualTo(TriggerSourceType.USER)
          },
        )
      }

      verify(exactly = 1) {
        domainEventWorkerMock.emitEvent(
          match {
            it.eventType == type.typeName &&
              it.version == 1 &&
              it.description == type.typeDescription &&
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
        mockDomainEventUrlConfig.getUrlForDomainEventId(type, domainEventToSave.id)
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `saveAndEmit persists event and does not emit event if emit on domain event is false`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val cas1SpaceBookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type)

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope,
        bookingId = bookingId,
        cas1SpaceBookingId = cas1SpaceBookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
        emit = false,
      )

      domainEventService.saveAndEmitForEnvelope(domainEventToSave, type)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
            assertThat(it.cas1SpaceBookingId).isEqualTo(cas1SpaceBookingId)
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allNonEmittableCas1DomainEventTypes")
    fun `saveAndEmit does not emit event to SNS if event type not emittable`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type)

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val domainEventToSave = SaveCas1DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope,
        bookingId = bookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
      )

      domainEventService.saveAndEmitForEnvelope(domainEventToSave, type)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            assertThat(it.id).isEqualTo(id)
            assertThat(it.type).isEqualTo(type)
            assertThat(it.crn).isEqualTo(crn)
            assertThat(it.nomsNumber).isEqualTo(nomsNumber)
            assertThat(it.occurredAt.toInstant()).isEqualTo(occurredAt)
            assertThat(it.data).isEqualTo(domainEventAndJson.persistedJson)
            assertThat(it.triggeredByUserId).isEqualTo(user.id)
            assertThat(it.bookingId).isEqualTo(bookingId)
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1DomainEventServiceTest#allCas1DomainEventTypes")
    fun `saveAndEmit does not emit event to SNS if event fails to persist to database`(type: DomainEventType) {
      val id = UUID.randomUUID()
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.randomUUID()
      val crn = "CRN"
      val nomsNumber = "123"
      val occurredAt = Instant.now()
      val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(type)

      every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

      val domainEventToSave = SaveCas1DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt,
        data = domainEventAndJson.envelope,
        bookingId = bookingId,
        schemaVersion = domainEventAndJson.schemaVersion.versionNo,
      )

      try {
        domainEventService.saveAndEmitForEnvelope(domainEventToSave, type)
      } catch (_: Exception) {
      }

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          withArg {
            it.id == domainEventToSave.id &&
              it.type == type &&
              it.crn == crn &&
              it.nomsNumber == nomsNumber &&
              it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
              it.data == domainEventAndJson.persistedJson &&
              it.triggeredByUserId == user.id &&
              it.bookingId == bookingId
          },
        )
      }

      verify(exactly = 0) {
        domainEventWorkerMock.emitEvent(any(), any())
      }
    }
  }

  @Nested
  inner class Replay {

    @Test
    fun `replay throws exception if domain event not found`() {
      val id = UUID.fromString("d37c9dd3-8ec8-4362-9525-1e85bcb36e79")

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

      assertThatThrownBy {
        domainEventService.replay(id)
      }.hasMessage("Not Found: No DomainEvent with an ID of d37c9dd3-8ec8-4362-9525-1e85bcb36e79 could be found")
    }

    @Test
    fun `replay doesnt emit event if emitting events is disabled`() {
      val id = UUID.fromString("9785dd29-0e1f-467a-933e-5e731cc27adc")
      val type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.fromString("8785dd29-0e1f-467a-933e-5e731cc27adc")
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"
      val occurredAt = OffsetDateTime.now()

      val domainEventEntity = DomainEventEntityFactory()
        .withId(id)
        .withType(type)
        .withApplicationId(applicationId)
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .withOccurredAt(occurredAt)
        .withBookingId(bookingId)
        .produce()

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns domainEventEntity
      every { mockDomainEventUrlConfig.getUrlForDomainEventId(type, id) } returns "theCorrectUrl"

      domainEventServiceEmitDisabled.replay(id)

      verify { domainEventWorkerMock wasNot Called }
    }

    @Test
    fun `replay emits event to SNS`() {
      val id = UUID.fromString("9785dd29-0e1f-467a-933e-5e731cc27adc")
      val type = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED
      val applicationId = UUID.randomUUID()
      val bookingId = UUID.fromString("8785dd29-0e1f-467a-933e-5e731cc27adc")
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"
      val occurredAt = OffsetDateTime.now()

      val domainEventEntity = DomainEventEntityFactory()
        .withId(id)
        .withType(type)
        .withApplicationId(applicationId)
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .withOccurredAt(occurredAt)
        .withBookingId(bookingId)
        .produce()

      every { domainEventRepositoryMock.findByIdOrNull(id) } returns domainEventEntity
      every { mockDomainEventUrlConfig.getUrlForDomainEventId(type, id) } returns "theCorrectUrl"
      every { domainEventWorkerMock.emitEvent(any(), any()) } returns Unit

      domainEventService.replay(id)

      verify(exactly = 1) {
        domainEventWorkerMock.emitEvent(
          withArg { snsEvent ->
            assertThat(snsEvent.eventType).isEqualTo(type.typeName)
            assertThat(snsEvent.version).isEqualTo(1)
            assertThat(snsEvent.description).isEqualTo(type.typeDescription)
            assertThat(snsEvent.detailUrl).isEqualTo("theCorrectUrl")
            assertThat(snsEvent.occurredAt).isEqualTo(occurredAt)
            assertThat(snsEvent.additionalInformation.applicationId).isEqualTo(applicationId)
            assertThat(snsEvent.personReference.identifiers.first { it.type == "CRN" }.value).isEqualTo(crn)
            assertThat(snsEvent.personReference.identifiers.first { it.type == "NOMS" }.value).isEqualTo(nomsNumber)
          },
          id,
        )
      }
    }
  }

  @Nested
  inner class SaveDomainEvents {

    @Test
    fun `saveApplicationSubmittedDomainEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = ApplicationSubmittedFactory().produce()
      val domainEventEnvelope = mockk<ApplicationSubmittedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<ApplicationSubmittedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveApplicationSubmittedDomainEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
        )
      }
    }

    @Test
    fun `saveApplicationAssessedDomainEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = ApplicationAssessedFactory().produce()
      val domainEventEnvelope = mockk<ApplicationAssessedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<ApplicationAssessedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveApplicationAssessedDomainEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
        )
      }
    }

    @Test
    fun `saveBookingMadeDomainEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = BookingMadeFactory().produce()
      val domainEventEnvelope = mockk<BookingMadeEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<BookingMadeEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveBookingMadeDomainEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
        )
      }
    }

    @Test
    fun `savePersonArrivedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = PersonArrivedFactory().produce()
      val domainEventEnvelope = mockk<PersonArrivedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<PersonArrivedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.savePersonArrivedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
        )
      }
    }

    @Test
    fun `savePersonNotArrivedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = PersonNotArrivedFactory().produce()
      val domainEventEnvelope = mockk<PersonNotArrivedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<PersonNotArrivedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.savePersonNotArrivedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
        )
      }
    }

    @Test
    fun `savePersonDepartedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = PersonDepartedFactory().produce()
      val domainEventEnvelope = mockk<PersonDepartedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<PersonDepartedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.savePersonDepartedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
        )
      }
    }

    @Test
    fun `saveBookingNotMadeEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = BookingNotMadeFactory().produce()
      val domainEventEnvelope = mockk<BookingNotMadeEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<BookingNotMadeEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveBookingNotMadeEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
        )
      }
    }

    @Test
    fun `saveBookingCancelledEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = BookingCancelledFactory().produce()
      val domainEventEnvelope = mockk<BookingCancelledEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<BookingCancelledEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveBookingCancelledEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
        )
      }
    }

    @Test
    fun `saveBookingChangedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = BookingChangedFactory().produce()
      val domainEventEnvelope = mockk<BookingChangedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<BookingChangedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveBookingChangedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
        )
      }
    }

    @Test
    fun `saveBookingKeyWorkerAssignedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = BookingKeyWorkerAssignedFactory().produce()
      val domainEventEnvelope = mockk<BookingKeyWorkerAssignedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<BookingKeyWorkerAssignedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveKeyWorkerAssignedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED,
        )
      }
    }

    @Test
    fun `saveApplicationWithdrawnEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = ApplicationWithdrawnFactory().produce()
      val domainEventEnvelope = mockk<ApplicationWithdrawnEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<ApplicationWithdrawnEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveApplicationWithdrawnEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
        )
      }
    }

    @Test
    fun `saveAssessmentAppealedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = AssessmentAppealedFactory().produce()
      val domainEventEnvelope = mockk<AssessmentAppealedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<AssessmentAppealedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveAssessmentAppealedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
        )
      }
    }

    @Test
    fun `savePlacementApplicationWithdrawnEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = PlacementApplicationWithdrawnFactory().produce()
      val domainEventEnvelope = mockk<PlacementApplicationWithdrawnEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<PlacementApplicationWithdrawnEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.savePlacementApplicationWithdrawnEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
        )
      }
    }

    @Test
    fun `savePlacementApplicationAllocatedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = PlacementApplicationAllocatedFactory().produce()
      val domainEventEnvelope = mockk<PlacementApplicationAllocatedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<PlacementApplicationAllocatedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.savePlacementApplicationAllocatedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
        )
      }
    }

    @Test
    fun `savePlacementApplicationDecisionEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = RequestForPlacementAssessedFactory().produce()
      val domainEventEnvelope = mockk<RequestForPlacementAssessedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<RequestForPlacementAssessedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveRequestForPlacementAssessedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED,
        )
      }
    }

    @Test
    fun `saveMatchRequestWithdrawnEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = MatchRequestWithdrawnFactory().produce()
      val domainEventEnvelope = mockk<MatchRequestWithdrawnEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<MatchRequestWithdrawnEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveMatchRequestWithdrawnEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
        )
      }
    }

    @Test
    fun `saveRequestForPlacementCreatedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = RequestForPlacementCreatedFactory().produce()
      val domainEventEnvelope = mockk<RequestForPlacementCreatedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<RequestForPlacementCreatedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveRequestForPlacementCreatedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
        )
      }
    }

    @Test
    fun `saveAssessmentAllocatedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = AssessmentAllocatedFactory().produce()
      val domainEventEnvelope = mockk<AssessmentAllocatedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<AssessmentAllocatedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveAssessmentAllocatedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
        )
      }
    }

    @Test
    fun `saveFurtherInformationRequestedEvent sends correct arguments to saveAndEmit`() {
      val id = UUID.randomUUID()

      val eventDetails = FurtherInformationRequestedFactory().produce()
      val domainEventEnvelope = mockk<FurtherInformationRequestedEnvelope>()
      val domainEvent = mockk<SaveCas1DomainEvent<FurtherInformationRequestedEnvelope>>()

      every { domainEvent.id } returns id
      every { domainEvent.data } returns domainEventEnvelope
      every { domainEventEnvelope.eventDetails } returns eventDetails

      val domainEventServiceSpy = spyk(domainEventService)

      every { domainEventServiceSpy.saveAndEmitForEnvelope(any(), any()) } returns Unit

      domainEventServiceSpy.saveFurtherInformationRequestedEvent(domainEvent)

      verify {
        domainEventServiceSpy.saveAndEmitForEnvelope(
          domainEvent = domainEvent,
          eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
        )
      }
    }
  }
}
