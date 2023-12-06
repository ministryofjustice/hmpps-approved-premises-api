package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.amazonaws.services.sns.model.PublishResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventServiceTest {
  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val hmppsQueueServiceMock = mockk<HmppsQueueService>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRepositoryMock,
    hmppsQueueService = hmppsQueueServiceMock,
    emitDomainEventsEnabled = true,
    cas2ApplicationSubmittedDetailUrlTemplate = "http://api/events/cas2/application-submitted/#eventId",
  )

  @Test
  fun `getApplicationSubmittedEvent returns null when event does not exist`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns null

    assertThat(domainEventService.getCas2ApplicationSubmittedDomainEvent(id)).isNull()
  }

  @Test
  fun `getApplicationSubmittedEvent returns event`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val data = Cas2ApplicationSubmittedEvent(
      id = id,
      timestamp = occurredAt.toInstant(),
      eventType = EventType.applicationSubmitted,
      eventDetails = Cas2ApplicationSubmittedEventDetailsFactory().produce(),
    )

    every { domainEventRepositoryMock.findByIdOrNull(id) } returns DomainEventEntityFactory()
      .withId(id)
      .withApplicationId(applicationId)
      .withCrn(crn)
      .withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
      .withData(objectMapper.writeValueAsString(data))
      .withOccurredAt(occurredAt)
      .produce()

    val event = domainEventService.getCas2ApplicationSubmittedDomainEvent(id)
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
  fun `saveApplicationSubmittedEvent persists event, emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = Cas2ApplicationSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationSubmitted,
        eventDetails = Cas2ApplicationSubmittedEventDetailsFactory().produce(),
      ),
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    domainEventService.saveCas2ApplicationSubmittedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS2_APPLICATION_SUBMITTED &&
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

          deserializedMessage.eventType == "applications.cas2.application.submitted" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "An application has been submitted for a CAS2 placement" &&
            deserializedMessage.detailUrl == "http://api/events/cas2/application-submitted/$id" &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms } &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn }
        },
      )
    }
  }

  @Test
  fun `saveApplicationSubmittedEvent does not emit event to SNS if event fails to persist to database`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRepositoryMock.save(any()) } throws RuntimeException("A database exception")

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = Cas2ApplicationSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationSubmitted,
        eventDetails = Cas2ApplicationSubmittedEventDetailsFactory().produce(),
      ),
    )

    every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { mockHmppsTopic.snsClient.publish(any()) } returns PublishResult()

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.saveCas2ApplicationSubmittedDomainEvent(domainEventToSave) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS2_APPLICATION_SUBMITTED &&
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
  fun `saveApplicationSubmittedEvent does not emit if emitDomainEventsEnabled is false`() {
    val domainEventServiceDisabled = DomainEventService(
      objectMapper = objectMapper,
      domainEventRepository = domainEventRepositoryMock,
      hmppsQueueService = hmppsQueueServiceMock,
      emitDomainEventsEnabled = false,
      cas2ApplicationSubmittedDetailUrlTemplate = "http://api/events/cas2/application-submitted/#eventId",
    )

    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domain-events") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = Cas2ApplicationSubmittedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.applicationSubmitted,
        eventDetails = Cas2ApplicationSubmittedEventDetailsFactory().produce(),
      ),
    )

    domainEventServiceDisabled.saveCas2ApplicationSubmittedDomainEvent(domainEventToSave)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS2_APPLICATION_SUBMITTED &&
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
}
