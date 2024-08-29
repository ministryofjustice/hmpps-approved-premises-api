package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import com.amazonaws.services.sns.model.InternalErrorException
import com.amazonaws.services.sns.model.PublishResult
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3BookingCancelledEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3BookingConfirmedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3BookingProvisionallyMadeEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3PersonArrivedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3PersonDepartedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3ReferralSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
@SuppressWarnings("CyclomaticComplexMethod")
class DomainEventServiceTest {
  @MockK
  lateinit var domainEventRepositoryMock: DomainEventRepository

  @MockK
  lateinit var domainEventBuilderMock: DomainEventBuilder

  @MockK
  lateinit var hmppsQueueServiceMock: HmppsQueueService

  @MockK
  lateinit var mockDomainEventUrlConfig: DomainEventUrlConfig

  @MockK
  lateinit var domainEventServiceConfig: DomainEventServiceConfig

  @MockK
  lateinit var userService: UserService

  @InjectMockKs
  private lateinit var domainEventService: DomainEventService

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
    every { domainEventServiceConfig.emitForEvent(any()) } returns true
    every { mockDomainEventUrlConfig.getUrlForDomainEventId(any(), any()) } returns detailUrl
    every { userService.getUserForRequestOrNull() } returns user
  }

  @Test
  fun `getBookingCancelledEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingCancelledEvent(id)).isNull()
  }

  @Test
  fun `getBookingCancelledEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingCancelledEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingCancelled,
      eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_CANCELLED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingCancelledEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getBookingCancelledEvent returns event without additional staff detail`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingCancelledEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingCancelled,
      eventDetails = CAS3BookingCancelledEventDetailsFactory()
        .withCancelledBy(null)
        .produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_CANCELLED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingCancelledEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
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

    every { domainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    domainEventService.saveBookingCancelledEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.booking.cancelled" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A booking for a Transitional Accommodation premises has been cancelled" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

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

    every { domainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.saveBookingCancelledEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data)
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
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

    every { domainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveBookingCancelledEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getBookingConfirmedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingConfirmedEvent(id)).isNull()
  }

  @Test
  fun `getBookingConfirmedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingConfirmedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingConfirmed,
      eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_CONFIRMED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingConfirmedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getBookingConfirmedEvent returns event without optional staff detail`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingConfirmedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingConfirmed,
      eventDetails = CAS3BookingConfirmedEventDetailsFactory()
        .withConfirmedBy(null)
        .produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_CONFIRMED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingConfirmedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingConfirmedEvent persists event, emits event to SNS`() {
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
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    domainEventService.saveBookingConfirmedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CONFIRMED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.booking.confirmed" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A booking has been confirmed for a Transitional Accommodation premises" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CONFIRMED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingConfirmedEvent persists event, but does not emit event to SNS when event is disabled`() {
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
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.saveBookingConfirmedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CONFIRMED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `saveBookingConfirmedEvent does not emit event to SNS if event fails to persist to database`() {
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
      data = CAS3BookingConfirmedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.bookingConfirmed,
        eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveBookingConfirmedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CONFIRMED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getBookingProvisionallyMadeEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingProvisionallyMadeEvent(id)).isNull()
  }

  @Test
  fun `getBookingProvisionallyMadeEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingProvisionallyMadeEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingProvisionallyMade,
      eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingProvisionallyMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getBookingProvisionallyMadeEvent returns event with additional staff details`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingProvisionallyMadeEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingProvisionallyMade,
      eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
        .withBookedBy(StaffMemberFactory().produce())
        .produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingProvisionallyMadeEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveBookingProvisionallyMadeEvent persists event, emits event to SNS`() {
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

    every { domainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    domainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.booking.provisionally-made" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A booking has been provisionally made for a Transitional Accommodation premises" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingProvisionallyMadeEvent persists event, but does not emit event to SNS when event is disabled`() {
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

    every { domainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `saveBookingProvisionallyMadeEvent does not emit event to SNS if event fails to persist to database`() {
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

    every { domainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getPersonArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonArrivedEvent(id)).isNull()
  }

  @Test
  fun `getPersonArrivedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3PersonArrivedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personArrived,
      eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getPersonArrivedEvent returns event without staff detail`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3PersonArrivedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personArrived,
      eventDetails = CAS3PersonArrivedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_ARRIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
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

    every { domainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    domainEventService.savePersonArrivedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.person.arrived" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has arrived at a Transitional Accommodation premises for their booking" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

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

    every { domainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.savePersonArrivedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
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

    every { domainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.savePersonArrivedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getPersonDepartedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonDepartedEvent(id)).isNull()
  }

  @Test
  fun `getPersonDepartedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3PersonDepartedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personDeparted,
      eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_DEPARTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonDepartedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
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
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    domainEventService.savePersonDepartedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.person.departed" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has left a Transitional Accommodation premises" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTED, domainEventToSave.id)
    }
  }

  @Test
  fun `savePersonDepartedEvent persists event, but does not emit event to SNS when event is disabled`() {
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
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.savePersonDepartedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `savePersonDepartedEvent does not emit event to SNS if event fails to persist to database`() {
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
      data = CAS3PersonDepartedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personDeparted,
        eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.savePersonDepartedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getReferralSubmittedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getReferralSubmittedEvent(id)).isNull()
  }

  @Test
  fun `getReferralSubmittedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3ReferralSubmittedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.referralSubmitted,
      eventDetails = CAS3ReferralSubmittedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_REFERRAL_SUBMITTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getReferralSubmittedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `saveReferralSubmittedEvent persists event, emits event to SNS`() {
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
      data = CAS3ReferralSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.referralSubmitted,
        eventDetails = CAS3ReferralSubmittedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

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

    domainEventService.saveReferralSubmittedEvent(applicationEntity)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_REFERRAL_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.referral.submitted" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A referral for Transitional Accommodation has been submitted" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_REFERRAL_SUBMITTED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveReferralSubmittedEvent persists event, but does not emit event to SNS when event is disabled`() {
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
      data = CAS3ReferralSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.referralSubmitted,
        eventDetails = CAS3ReferralSubmittedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

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

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.saveReferralSubmittedEvent(applicationEntity)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_REFERRAL_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `saveReferralSubmittedEvent does not emit event to SNS if event fails to persist to database`() {
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
      data = CAS3ReferralSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.referralSubmitted,
        eventDetails = CAS3ReferralSubmittedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

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

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveReferralSubmittedEvent(applicationEntity) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_REFERRAL_SUBMITTED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `should savePersonDepartureUpdatedEvent persists given event into DB and emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.person.departed.updated" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Person has updated departure date of Transitional Accommodation premises" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `should not emit SNS event when savePersonDepartureUpdatedEvent persists event fail to store to DB`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `Should throw error when savePersonDepartureUpdatedEvent fail to publish and save event in DB`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } throws InternalErrorException("Unexpected exception")

    assertThatExceptionOfType(InternalErrorException::class.java)
      .isThrownBy { domainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.person.departed.updated" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Person has updated departure date of Transitional Accommodation premises" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `Should getPersonDepartureUpdatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonDepartureUpdatedEvent(id)).isNull()
  }

  @Test
  fun `Should getPersonDepartureUpdatedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val data = CAS3PersonDepartureUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personDepartureUpdated,
      eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
    )
    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonDepartureUpdatedEvent(id)

    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `should savePersonDepartureUpdatedEvent persists given event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCAS3DepartureUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()
    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCancelledUpdatedEventEntity(id, applicationId, crn, nomsNumber, occurredAt, StaffMemberFactory().produce())
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.booking.cancelled.updated" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A cancelled booking for a Transitional Accommodation premises has been updated" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event without user entity, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCancelledUpdatedEventEntity(id, applicationId, crn, nomsNumber, occurredAt, null)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), any()) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.booking.cancelled.updated" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "A cancelled booking for a Transitional Accommodation premises has been updated" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent persists event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCancelledUpdatedEventEntity(id, applicationId, crn, nomsNumber, occurredAt, StaffMemberFactory().produce())
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `saveBookingCancelledUpdatedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val domainEventToSave = createCancelledUpdatedEventEntity(id, applicationId, crn, nomsNumber, occurredAt, StaffMemberFactory().produce())
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    val mockHmppsTopic = mockk<HmppsTopic>()
    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 0) {
      mockHmppsTopic.snsClient.publish(any())
    }
  }

  @Test
  fun `getBookingCancelledUpdatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getBookingCancelledUpdatedEvent(id)).isNull()
  }

  @Test
  fun `getBookingCancelledUpdatedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3BookingCancelledUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bookingCancelledUpdated,
      eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getBookingCancelledUpdatedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `savePersonArrivedUpdatedEvent persists updated arrival event and emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = createArrivedUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber
          it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }

    verify(exactly = 1) {
      mockHmppsTopic.snsClient.publish(
        match {
          val deserializedMessage = objectMapper.readValue(it.message, SnsEvent::class.java)

          deserializedMessage.eventType == "accommodation.cas3.person.arrived.updated" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has changed arrival date at a Transitional Accommodation premises for their booking" &&
            deserializedMessage.detailUrl == detailUrl &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }

    verify(exactly = 1) {
      mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS3_PERSON_ARRIVED_UPDATED, domainEventToSave.id)
    }
  }

  @Test
  fun `savePersonArrivedUpdatedEvent persists updated arrival event, but does not emit event to SNS when event is disabled`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = createArrivedUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    every { domainEventServiceConfig.emitForEvent(any()) } returns false
    domainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
    verify(exactly = 0) { mockHmppsTopic.snsClient.publish(any()) }
  }

  @Test
  fun `savePersonArrivedUpdatedEvent doesn't persist event when database exception occured and not emit SNS domain event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"
    val mockHmppsTopic = mockk<HmppsTopic>()
    val domainEventToSave = createArrivedUpdatedDomainEvent(id, applicationId, crn, nomsNumber, occurredAt)
    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")
    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic
    every { domainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED_UPDATED &&
            it.crn == domainEventToSave.crn &&
            it.nomsNumber == domainEventToSave.nomsNumber &&
            it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
    verify(exactly = 0) { mockHmppsTopic.snsClient.publish(any()) }
  }

  @Test
  fun `getPersonArrivedUpdatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getPersonArrivedUpdatedEvent(id)).isNull()
  }

  @Test
  fun `getPersonArrivedUpdatedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3PersonArrivedUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personArrivedUpdated,
      eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_ARRIVED_UPDATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedUpdatedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  @Test
  fun `getPersonArrivedUpdatedEvent returns event without staff detail`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"
    val nomsNumber = "theNomsNumber"

    val data = CAS3PersonArrivedUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personArrivedUpdated,
      eventDetails = CAS3PersonArrivedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withNomsNumber(nomsNumber)
      .withType(DomainEventType.CAS3_PERSON_ARRIVED_UPDATED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getPersonArrivedUpdatedEvent(id)
    assertThat(event).isEqualTo(
      DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = "CRN",
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = data,
      ),
    )
  }

  private fun createArrivedUpdatedDomainEvent(
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
    data = CAS3PersonArrivedUpdatedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.personArrived,
      eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
    ),
  )

  private fun createTemporaryAccommodationPremisesBookingEntity(): BookingEntity {
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

    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(probationRegion)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(null)
      .withApplication(applicationEntity)
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
  ): DomainEvent<CAS3BookingCancelledUpdatedEvent> {
    return DomainEvent(
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
}
