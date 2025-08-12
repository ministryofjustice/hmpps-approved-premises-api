package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.service.v2

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.services.sns.model.InternalErrorException
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3PremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingCancelledEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingConfirmedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingProvisionallyMadeEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonArrivedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonDepartedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3DomainEventServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

@ExtendWith(MockKExtension::class)
@SuppressWarnings("CyclomaticComplexMethod", "LargeClass")
class Cas3v2DomainEventServiceTest {
  @MockK
  private lateinit var domainEventRepositoryMock: DomainEventRepository

  @MockK
  private lateinit var cas3DomainEventBuilderMock: Cas3v2DomainEventBuilder

  @MockK
  private lateinit var hmppsQueueServiceMock: HmppsQueueService

  @MockK
  private lateinit var mockDomainEventUrlConfig: DomainEventUrlConfig

  @MockK
  private lateinit var cas3DomainEventServiceConfig: Cas3DomainEventServiceConfig

  @MockK
  private lateinit var userService: UserService

  @InjectMockKs
  private lateinit var cas3DomainEventService: Cas3v2DomainEventService

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val user = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val detailUrl = "http://example.com/123"

  @BeforeEach
  fun setup() {
    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns true
    every { mockDomainEventUrlConfig.getUrlForDomainEventId(any(), any()) } returns detailUrl
    every { userService.getUserForRequestOrNull() } returns user
  }

  @Test
  fun `saveBookingCancelledEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingCancelledEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingCancelled,
        eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createCas3PremisesBookingEntity()

    cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.booking.cancelled") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("A booking for a Transitional Accommodation premises has been cancelled") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("CRN")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
        }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("NOMS")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
        }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CANCELLED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingCancelledEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingCancelledEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingCancelled,
        eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createCas3PremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveBookingCancelledEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingCancelledEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingCancelled,
        eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createCas3PremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveBookingConfirmedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CONFIRMED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)
    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.booking.confirmed") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("A booking has been confirmed for a Transitional Accommodation premises") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      {
        assertThat(
          deserializedMessage.personReference.identifiers.any {
            it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn
          },
        ).isTrue()
      },
      {
        assertThat(
          deserializedMessage.personReference.identifiers.any {
            it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms
          },
        ).isTrue()
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CONFIRMED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingConfirmedEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()
    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false

    cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CONFIRMED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveBookingConfirmedEven does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CONFIRMED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveCas3BookingProvisionallyMadeEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingProvisionallyMadeEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingProvisionallyMade,
        eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
          .withBookedBy(StaffMemberFactory().produce())
          .produce(),
      ),
    )

    val bookingEntity = createCas3PremisesBookingEntity()

    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    val publishSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishSlot))
    }

    val deserializedMessage = objectMapper.readValue(publishSlot.captured.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.booking.provisionally-made") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      {
        assertThat(deserializedMessage.description)
          .isEqualTo("A booking has been provisionally made for a Transitional Accommodation premises")
      },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("CRN")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
          }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("NOMS")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
          }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE, domainEventToSave.id)
    }
  }

  @Test
  fun `saveCas3BookingProvisionallyMadeEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingProvisionallyMadeEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingProvisionallyMade,
        eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
          .withBookedBy(StaffMemberFactory().produce())
          .produce(),
      ),
    )

    val bookingEntity = createCas3PremisesBookingEntity()

    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()
    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false

    cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveCas3BookingProvisionallyMadeEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3BookingProvisionallyMadeEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingProvisionallyMade,
        eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
          .withBookedBy(StaffMemberFactory().produce())
          .produce(),
      ),
    )

    val bookingEntity = createCas3PremisesBookingEntity()

    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveCas3BookingProvisionallyMadeEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `savePersonArrivedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3PersonArrivedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createCas3PremisesBookingEntity()

    cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_ARRIVED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.person.arrived") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("Someone has arrived at a Transitional Accommodation premises for their booking") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("CRN")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
          }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("NOMS")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
          }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_ARRIVED, domainEventToSave.id)
    }
  }

  @Test
  fun `savePersonArrivedEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3PersonArrivedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createCas3PremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_ARRIVED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `savePersonArrivedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      nomsNumber = nomsNumber,
      occurredAt = Instant.now(),
      data = CAS3PersonArrivedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createCas3PremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_ARRIVED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `savePersonDepartedEvent persists event, emits event to SNS`() {
    val id = UUID.randomUUID()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.randomUUID(),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createCas3PremisesBookingEntity()

    cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.person.departed") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("Someone has left a Transitional Accommodation premises") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      { assertThat(deserializedMessage.personReference.identifiers).anyMatch { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } },
      { assertThat(deserializedMessage.personReference.identifiers).anyMatch { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms } },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTED, domainEventToSave.id)
    }
  }

  @Test
  fun `savePersonDepartedEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.randomUUID()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.randomUUID(),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()
    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false

    val bookingEntity = createCas3PremisesBookingEntity()

    cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `savePersonDepartedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.randomUUID()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = UUID.randomUUID(),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = Instant.now(),
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = OffsetDateTime.parse("2023-02-01T14:03:00+00:00").toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createCas3PremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `should savePersonDepartureUpdatedEvent persists given event into DB and emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(
      id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
    )
    val bookingEntity = createCas3PremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    val publishedRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishedRequestSlot))
    }

    val deserializedMessage = objectMapper.readValue(publishedRequestSlot.captured.message(), SnsEvent::class.java)
    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.person.departed.updated") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("Person has updated departure date of Transitional Accommodation premises") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("CRN")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
        }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("NOMS")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
        }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `should not emit SNS event when savePersonDepartureUpdatedEvent persists event fail to store to DB`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(
      id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
    )
    val bookingEntity = createCas3PremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

    val domainEventSlot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(domainEventSlot))
    }

    val savedEvent = domainEventSlot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `Should throw error when savePersonDepartureUpdatedEvent fail  to publish and save event in DB`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(
      id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
    )
    val bookingEntity = createCas3PremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } throws InternalErrorException.builder().message("Unexpected exception").build()

    assertThatExceptionOfType(InternalErrorException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.person.departed.updated") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("Person has updated departure date of Transitional Accommodation premises") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("CRN")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
        }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers).anySatisfy {
          assertThat(it.type).isEqualTo("NOMS")
          assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
        }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `should savePersonDepartureUpdatedEvent persists given event but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(
      id,
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
    )

    val bookingEntity = createCas3PremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()

    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event, emits event to SNS`() {
    val domainEventToSave = createCancelledUpdatedEventEntity(
      id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981"),
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
      staffMember = StaffMemberFactory().produce(),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())
    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.booking.cancelled.updated") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("A cancelled booking for a Transitional Accommodation premises has been updated") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("CRN")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
          }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("NOMS")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
          }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event without user entity, emits event to SNS`() {
    val domainEventToSave = createCancelledUpdatedEventEntity(
      id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981"),
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
      staffMember = null,
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(Cas3BookingEntity::class), any()) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(
      PublishResponse.builder().build(),
    )

    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
    )

    val publishRequestSlot = slot<PublishRequest>()
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(capture(publishRequestSlot))
    }

    val capturedRequest = publishRequestSlot.captured
    val deserializedMessage = objectMapper.readValue(capturedRequest.message(), SnsEvent::class.java)

    assertAll(
      { assertThat(deserializedMessage.eventType).isEqualTo("accommodation.cas3.booking.cancelled.updated") },
      { assertThat(deserializedMessage.version).isEqualTo(1) },
      { assertThat(deserializedMessage.description).isEqualTo("A cancelled booking for a Transitional Accommodation premises has been updated") },
      { assertThat(deserializedMessage.detailUrl).isEqualTo(detailUrl) },
      { assertThat(deserializedMessage.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(deserializedMessage.additionalInformation.applicationId).isEqualTo(domainEventToSave.applicationId) },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("CRN")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.crn)
          }
      },
      {
        assertThat(deserializedMessage.personReference.identifiers)
          .anySatisfy {
            assertThat(it.type).isEqualTo("NOMS")
            assertThat(it.value).isEqualTo(domainEventToSave.data.eventDetails.personReference.noms)
          }
      },
    )

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event, but does not emit event to SNS when event is disabled`() {
    val domainEventToSave = createCancelledUpdatedEventEntity(
      id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981"),
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
      staffMember = StaffMemberFactory().produce(),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.nomsNumber).isEqualTo(domainEventToSave.nomsNumber) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent does not emit event to SNS if event fails to persist to database`() {
    val domainEventToSave = createCancelledUpdatedEventEntity(
      id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981"),
      applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b"),
      crn = "CRN",
      nomsNumber = "theNomsNumber",
      occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00"),
      staffMember = StaffMemberFactory().produce(),
    )
    val bookingEntity = createCas3PremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(Cas3BookingEntity::class), user) } returns domainEventToSave

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user) }

    val slot = slot<DomainEventEntity>()
    verify(exactly = 1) {
      domainEventRepositoryMock.save(capture(slot))
    }

    val savedEvent = slot.captured
    assertAll(
      { assertThat(savedEvent.id).isEqualTo(domainEventToSave.id) },
      { assertThat(savedEvent.type).isEqualTo(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED) },
      { assertThat(savedEvent.crn).isEqualTo(domainEventToSave.crn) },
      { assertThat(savedEvent.occurredAt.toInstant()).isEqualTo(domainEventToSave.occurredAt) },
      { assertThat(savedEvent.data).isEqualTo(objectMapper.writeValueAsString(domainEventToSave.data)) },
      { assertThat(savedEvent.triggeredByUserId).isEqualTo(user.id) },
      { assertThat(savedEvent.triggerSource).isEqualTo(TriggerSourceType.USER) },
    )

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  private fun createCas3PremisesBookingEntity(): Cas3BookingEntity {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val applicationEntity = TemporaryAccommodationApplicationEntityFactory()
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce()
      }
      .withProbationRegion(probationRegion)
      .produce()

    val premises = Cas3PremisesEntityFactory()
      .withProbationDeliveryUnit(
        ProbationDeliveryUnitEntityFactory()
          .withProbationRegion(probationRegion)
          .produce(),
      )
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val bookingEntity = Cas3BookingEntityFactory()
      .withPremises(premises)
      .withApplication(applicationEntity)
      .withBedspace(
        Cas3BedspaceEntityFactory()
          .withPremises(premises)
          .produce(),
      )
      .produce()
    return bookingEntity
  }

  private fun createCAS3DepartureUpdatedDomainEvent(
    id: UUID,
    applicationId: UUID?,
    crn: String,
    nomsNumber: String,
    occurredAt: OffsetDateTime,
  ) = DomainEvent(
    id = id,
    applicationId = applicationId,
    crn = crn,
    nomsNumber = nomsNumber,
    occurredAt = Instant.now(),
    data = CAS3PersonDepartureUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personDepartureUpdated,
      eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
    ),
  )

  @SuppressWarnings("LongParameterList")
  private fun createCancelledUpdatedEventEntity(
    id: UUID,
    applicationId: UUID?,
    crn: String,
    nomsNumber: String,
    occurredAt: OffsetDateTime,
    staffMember: StaffMember?,
  ): DomainEvent<CAS3BookingCancelledUpdatedEvent> = DomainEvent(
    id = id,
    applicationId = applicationId,
    crn = crn,
    nomsNumber = nomsNumber,
    occurredAt = Instant.now(),
    data = CAS3BookingCancelledUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingCancelledUpdated,
      eventDetails = CAS3BookingCancelledEventDetailsFactory()
        .withCancelledBy(staffMember)
        .produce(),
    ),
  )
}
