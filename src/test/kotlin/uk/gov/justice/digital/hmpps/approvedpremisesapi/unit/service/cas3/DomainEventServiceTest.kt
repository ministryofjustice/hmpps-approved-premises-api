package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3.CAS3PersonArrivedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventServiceTest {
  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val domainEventBuilderMock = mockk<DomainEventBuilder>()
  private val hmppsQueueServiceMock = mockk<HmppsQueueService>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRepositoryMock,
    domainEventBuilder = domainEventBuilderMock,
    hmppsQueueService = hmppsQueueServiceMock,
    emitDomainEventsEnabled = true,
    personArrivedDetailUrlTemplate = "http://api/events/cas3/person-arrived/#eventId",
    personDepartedDetailUrlTemplate = "http://api/events/cas3/person-departed/#eventId",
  )

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

    every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

    val mockHmppsTopic = mockk<HmppsTopic>()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

    val domainEventToSave = DomainEvent(
      id = id,
      applicationId = applicationId,
      crn = crn,
      occurredAt = Instant.now(),
      data = CAS3PersonArrivedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getPersonArrivedDomainEvent(any()) } returns domainEventToSave

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

    domainEventService.savePersonArrivedEvent(bookingEntity)

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED &&
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

          deserializedMessage.eventType == "accommodation.cas3.person.arrived" &&
            deserializedMessage.version == 1 &&
            deserializedMessage.description == "Someone has arrived at a Transitional Accommodation premises for their booking" &&
            deserializedMessage.detailUrl == "http://api/events/cas3/person-arrived/$id" &&
            deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
            deserializedMessage.additionalInformation.applicationId == applicationId &&
            deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn } &&
            deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms }
        },
      )
    }
  }

  @Test
  fun `savePersonArrivedEvent does not emit event to SNS if event fails to persist to database`() {
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
      data = CAS3PersonArrivedEvent(
        id = id,
        timestamp = occurredAt.toInstant(),
        eventType = EventType.personArrived,
        eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
      ),
    )

    every { domainEventBuilderMock.getPersonArrivedDomainEvent(any()) } returns domainEventToSave

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

    assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy { domainEventService.savePersonArrivedEvent(bookingEntity) }

    verify(exactly = 1) {
      domainEventRepositoryMock.save(
        match {
          it.id == domainEventToSave.id &&
            it.type == DomainEventType.CAS3_PERSON_ARRIVED &&
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
