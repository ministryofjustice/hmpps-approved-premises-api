package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

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
import software.amazon.awssdk.services.sns.model.InternalErrorException
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedField
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3PremisesEntityFactory
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3DomainEventServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

@ExtendWith(MockKExtension::class)
@SuppressWarnings("CyclomaticComplexMethod", "LargeClass")
class Cas3DomainEventServiceTest {
  @MockK
  lateinit var domainEventRepositoryMock: DomainEventRepository

  @MockK
  lateinit var cas3DomainEventBuilderMock: Cas3DomainEventBuilder

  @MockK
  lateinit var hmppsQueueServiceMock: HmppsQueueService

  @MockK
  lateinit var mockDomainEventUrlConfig: DomainEventUrlConfig

  @MockK
  lateinit var cas3DomainEventServiceConfig: Cas3DomainEventServiceConfig

  @MockK
  lateinit var userService: UserService

  @InjectMockKs
  private lateinit var cas3DomainEventService: Cas3DomainEventService

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
  fun `getBookingCancelledEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getBookingCancelledEvent(id)).isNull()
  }

  @Test
  fun `getLastBedspaceUnarchiveEventDetails returns null when event does not exist`() {
    val id = UUID.randomUUID()

    every { domainEventRepositoryMock.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(id, DomainEventType.CAS3_BEDSPACE_UNARCHIVED) } returns null

    assertThat(cas3DomainEventService.getLastBedspaceUnarchiveEventDetails(id)).isNull()
  }

  @Test
  fun `getLastBedspaceUnarchiveEventDetails returns original start date and end date`() {
    val id = UUID.randomUUID()
    val bedspaceId = UUID.randomUUID()
    val occurredAt = OffsetDateTime.now()
    val newStartDate = LocalDate.now().plusDays(1)
    val currentStartDate = LocalDate.now().minusDays(2)
    val currentEndDate = LocalDate.now().minusDays(1)

    val data = CAS3BedspaceUnarchiveEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.bedspaceUnarchived,
      eventDetails = CAS3BedspaceUnarchiveEventDetails(bedspaceId, user.id, currentStartDate, currentEndDate, newStartDate),
    )

    every { domainEventRepositoryMock.findFirstByCas3BedspaceIdAndTypeOrderByCreatedAtDesc(bedspaceId, DomainEventType.CAS3_BEDSPACE_UNARCHIVED) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(null)
      .withCrn(null)
      .withCas3BedspaceId(bedspaceId)
      .withNomsNumber(null)
      .withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val eventDetails = cas3DomainEventService.getLastBedspaceUnarchiveEventDetails(bedspaceId)
    assertThat(eventDetails?.currentStartDate).isEqualTo(currentStartDate)
    assertThat(eventDetails?.currentEndDate).isEqualTo(currentEndDate)
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

    val event = cas3DomainEventService.getBookingCancelledEvent(id)
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

    val event = cas3DomainEventService.getBookingCancelledEvent(id)
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

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)

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

    every { cas3DomainEventBuilderMock.getBookingCancelledDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getBookingConfirmedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getBookingConfirmedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getBookingConfirmedEvent(id)
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

    val event = cas3DomainEventService.getBookingConfirmedEvent(id)
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

    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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

    every { cas3DomainEventBuilderMock.getBookingConfirmedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getBookingProvisionallyMadeEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getBookingProvisionallyMadeEvent(id)).isNull()
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

    val event = cas3DomainEventService.getBookingProvisionallyMadeEvent(id)
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

    val event = cas3DomainEventService.getBookingProvisionallyMadeEvent(id)
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

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()
    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()
    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false

    cas3DomainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventBuilderMock.getBookingProvisionallyMadeDomainEvent(eq(bookingEntity), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingProvisionallyMadeEvent(bookingEntity, user) }

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getPersonArrivedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getPersonArrivedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getPersonArrivedEvent(id)
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

    val event = cas3DomainEventService.getPersonArrivedEvent(id)
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

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)

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

    every { cas3DomainEventBuilderMock.getPersonArrivedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getPersonDepartedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getPersonDepartedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getPersonDepartedEvent(id)
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

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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

    every { cas3DomainEventBuilderMock.getPersonDepartedDomainEvent(any(), user) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    val bookingEntity = createTemporaryAccommodationPremisesBookingEntity()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartedEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getReferralSubmittedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getReferralSubmittedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getReferralSubmittedEvent(id)
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
  fun `saveBedspaceUnarchiveEvent saves event but does not emit it`() {
    val occuredAt = Instant.now()
    val bedspaceId = UUID.randomUUID()
    val newStartDate = LocalDate.now().plusDays(5)
    val currentStartDate = LocalDate.now().minusDays(20)
    val currentEndDate = LocalDate.now().minusDays(2)
    val id = UUID.randomUUID()
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      ).produce()
    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion).produce()
    val localAuthorityArea = LocalAuthorityAreaEntityFactory().produce()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .withProbationRegion(probationRegion)
      .produce()
    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()
    val room = RoomEntityFactory().withPremises(premises).produce()
    val bedspace = BedEntityFactory()
      .withId(bedspaceId)
      .withEndDate(null)
      .withStartDate(newStartDate)
      .withRoom(room)
      .produce()
    val eventDetails = CAS3BedspaceUnarchiveEventDetails(
      bedspaceId = bedspaceId,
      userId = user.id,
      newStartDate = newStartDate,
      currentStartDate = currentStartDate,
      currentEndDate = currentEndDate,
    )
    val data = CAS3BedspaceUnarchiveEvent(
      eventDetails = eventDetails,
      id = id,
      timestamp = occuredAt,
      eventType = EventType.bedspaceUnarchived,
    )
    val domainEventId = UUID.randomUUID()

    val domainEvent = DomainEvent(
      id = domainEventId,
      applicationId = null,
      bookingId = null,
      crn = null,
      nomsNumber = null,
      occurredAt = Instant.now(),
      data = data,
    )

    every { cas3DomainEventBuilderMock.getBedspaceUnarchiveEvent(eq(bedspace), eq(currentStartDate), eq(currentEndDate), eq(user)) } returns domainEvent
    every { domainEventRepositoryMock.save(any()) } returns null
    every { userService.getUserForRequest() } returns user
    every { userService.getUserForRequestOrNull() } returns user

    cas3DomainEventService.saveBedspaceUnarchiveEvent(bedspace, currentStartDate, currentEndDate)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEvent.id &&
            it.type == DomainEventType.CAS3_BEDSPACE_UNARCHIVED &&
            it.crn == domainEvent.crn &&
            it.cas3PremisesId == premises.id &&
            it.cas3BedspaceId == bedspaceId &&
            it.applicationId == null &&
            it.cas1SpaceBookingId == null &&
            it.assessmentId == null &&
            it.service == "CAS3"
          it.bookingId == null &&
            it.nomsNumber == null &&
            it.occurredAt.toInstant() == domainEvent.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEvent.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
  }

  @Test
  fun `saveBedspaceArchiveEvent saves event but does not emit it`() {
    val occuredAt = Instant.now()
    val bedspaceId = UUID.randomUUID()
    val endDate = LocalDate.parse("2021-01-01")
    val id = UUID.randomUUID()
    val probationRegion = ProbationRegionEntityFactory()
      .withApArea(
        ApAreaEntityFactory().produce(),
      ).produce()
    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withProbationRegion(probationRegion).produce()
    val localAuthorityArea = LocalAuthorityAreaEntityFactory().produce()
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withLocalAuthorityArea(localAuthorityArea)
      .withProbationDeliveryUnit(probationDeliveryUnit)
      .withProbationRegion(probationRegion)
      .produce()
    val user = UserEntityFactory()
      .withProbationRegion(probationRegion)
      .produce()
    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()
    val bedspace = BedEntityFactory()
      .withId(bedspaceId)
      .withEndDate(null)
      .withRoom(room)
      .produce()
    val eventDetails = CAS3BedspaceArchiveEventDetails(
      bedspaceId = bedspaceId,
      premisesId = premises.id,
      userId = user.id,
      endDate = endDate,
    )
    val data = CAS3BedspaceArchiveEvent(
      eventDetails = eventDetails,
      id = id,
      timestamp = occuredAt,
      eventType = EventType.bedspaceArchived,
    )
    val domainEventId = UUID.randomUUID()

    val domainEvent = DomainEvent(
      id = domainEventId,
      applicationId = null,
      bookingId = null,
      crn = null,
      nomsNumber = null,
      occurredAt = Instant.now(),
      data = data,
    )

    every { cas3DomainEventBuilderMock.getBedspaceArchiveEvent(eq(bedspace), eq(user)) } returns domainEvent
    every { domainEventRepositoryMock.save(any()) } returns null
    every { userService.getUserForRequest() } returns user
    every { userService.getUserForRequestOrNull() } returns user

    cas3DomainEventService.saveBedspaceArchiveEvent(bedspace)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEvent.id &&
            it.type == DomainEventType.CAS3_BEDSPACE_ARCHIVED &&
            it.crn == domainEvent.crn &&
            it.cas3PremisesId == premises.id &&
            it.cas3BedspaceId == bedspaceId &&
            it.applicationId == null &&
            it.cas1SpaceBookingId == null &&
            it.assessmentId == null &&
            it.service == "CAS3"
          it.bookingId == null &&
            it.nomsNumber == null &&
            it.occurredAt.toInstant() == domainEvent.occurredAt &&
            it.data == objectMapper.writeValueAsString(domainEvent.data) &&
            it.triggeredByUserId == user.id &&
            it.triggerSource == TriggerSourceType.USER
        },
      )
    }
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

    every { cas3DomainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

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

    cas3DomainEventService.saveReferralSubmittedEvent(applicationEntity)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    every { cas3DomainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

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

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveReferralSubmittedEvent(applicationEntity)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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

    every { cas3DomainEventBuilderMock.getReferralSubmittedDomainEvent(any()) } returns domainEventToSave

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

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
      .isThrownBy { cas3DomainEventService.saveReferralSubmittedEvent(applicationEntity) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } throws InternalErrorException.builder().message("Unexpected exception").build()

    assertThatExceptionOfType(InternalErrorException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user) }

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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

    assertThat(cas3DomainEventService.getPersonDepartureUpdatedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getPersonDepartureUpdatedEvent(id)

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
    every { cas3DomainEventBuilderMock.buildDepartureUpdatedDomainEvent(any(), user) } returns domainEventToSave

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonDepartureUpdatedEvent(bookingEntity, user)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())
    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), any()) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(
      PublishResponse.builder().build(),
    )

    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
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
    every { cas3DomainEventBuilderMock.getBookingCancelledUpdatedDomainEvent(any(), user) } returns domainEventToSave

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user) }

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
      mockHmppsTopic.snsClient.publish(any<PublishRequest>())
    }
  }

  @Test
  fun `getBookingCancelledUpdatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getBookingCancelledUpdatedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getBookingCancelledUpdatedEvent(id)
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
    every { cas3DomainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    cas3DomainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user)

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
        match<PublishRequest> {
          val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

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
  fun `getAssessmentUpdatedEvents returns CAS3_ASSESSMENT_UPDATED events`() {
    val uuid = UUID.randomUUID()
    every { domainEventRepositoryMock.findByAssessmentIdAndType(any(), any()) } returns emptyList()
    cas3DomainEventService.getAssessmentUpdatedEvents(uuid)
    verify(exactly = 1) {
      domainEventRepositoryMock.findByAssessmentIdAndType(uuid, DomainEventType.CAS3_ASSESSMENT_UPDATED)
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
    every { cas3DomainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave
    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture()

    every { cas3DomainEventServiceConfig.emitForEvent(any()) } returns false
    cas3DomainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user)

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
    verify(exactly = 0) { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) }
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
    every { cas3DomainEventBuilderMock.buildPersonArrivedUpdatedDomainEvent(any(), user) } returns domainEventToSave

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { cas3DomainEventService.savePersonArrivedUpdatedEvent(bookingEntity, user) }

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
    verify(exactly = 0) { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) }
  }

  @Test
  fun `getPersonArrivedUpdatedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(cas3DomainEventService.getPersonArrivedUpdatedEvent(id)).isNull()
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

    val event = cas3DomainEventService.getPersonArrivedUpdatedEvent(id)
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

    val event = cas3DomainEventService.getPersonArrivedUpdatedEvent(id)
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
  fun `saveAssessmentUpdatedEvent saves the domain event and does not emit to delius`() {
    every {
      cas3DomainEventBuilderMock.buildAssessmentUpdatedDomainEvent(any(), any())
    } answers { callOriginal() }

    val uuid = UUID.randomUUID()
    val application = TemporaryAccommodationApplicationEntityFactory().withDefaults().withCrn("C123456").produce()
    val assessment = TemporaryAccommodationAssessmentEntityFactory().withId(uuid).withApplication(application).produce()

    every { domainEventRepositoryMock.save(any()) } returnsArgument 0

    val updatedField = CAS3AssessmentUpdatedField("testField", "A", "B")
    val event = cas3DomainEventBuilderMock.buildAssessmentUpdatedDomainEvent(assessment, listOf(updatedField))

    cas3DomainEventService.saveAssessmentUpdatedEvent(event)

    verify(exactly = 1) { domainEventRepositoryMock.save(any(DomainEventEntity::class)) }
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
