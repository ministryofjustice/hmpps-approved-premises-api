package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AsyncDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SyncDomainEventWorker
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Suppress("SwallowedException")
class DomainEventWorkerTest {
  private val hmppsQueueServiceMock = mockk<HmppsQueueService>()
  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }
  private val hmppsTopicMock = mockk<HmppsTopic>()

  private val asyncDomainEventWorker = AsyncDomainEventWorker(
    domainTopic = hmppsTopicMock,
    objectMapper = objectMapper,
  )

  private val syncDomainEventWorker = SyncDomainEventWorker(
    domainTopic = hmppsTopicMock,
    objectMapper = objectMapper,
  )

  @BeforeEach
  fun clearConstructorMocks() {
    unmockkConstructor(AsyncDomainEventWorker::class)
    unmockkConstructor(SyncDomainEventWorker::class)
  }

  @Test
  fun `Creating a ConfiguredDomainEventWorker with asyncSaveEnabled set to true gives an AsyncDomainEventWorker`() {
    mockkConstructor(AsyncDomainEventWorker::class)

    val snsEvent = mockk<SnsEvent>()
    val domainEventId = UUID.randomUUID()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns hmppsTopicMock
    every { anyConstructed<AsyncDomainEventWorker>().emitEvent(snsEvent, domainEventId) } returns Unit

    val configuredWorker = ConfiguredDomainEventWorker(
      hmppsQueueServiceMock,
      objectMapper,
      true,
    )
    configuredWorker.emitEvent(snsEvent, domainEventId)

    verify {
      anyConstructed<AsyncDomainEventWorker>().emitEvent(snsEvent, domainEventId)
    }
  }

  @Test
  fun `Creating a ConfiguredDomainEventWorker with asyncSaveEnabled set to false gives a SyncDomainEventWorker`() {
    mockkConstructor(SyncDomainEventWorker::class)

    val snsEvent = mockk<SnsEvent>()
    val domainEventId = UUID.randomUUID()

    every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns hmppsTopicMock
    every { anyConstructed<SyncDomainEventWorker>().emitEvent(snsEvent, domainEventId) } returns Unit

    val configuredWorker = ConfiguredDomainEventWorker(
      hmppsQueueServiceMock,
      objectMapper,
      false,
    )
    configuredWorker.emitEvent(snsEvent, domainEventId)

    verify {
      anyConstructed<SyncDomainEventWorker>().emitEvent(snsEvent, domainEventId)
    }
  }

  @Test
  fun `async emit retries if it fails`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { asyncDomainEventWorker.domainTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"

    val snsEvent = SnsEvent(
      eventType = "",
      version = 1,
      description = "",
      detailUrl = "detailUrl",
      occurredAt = occurredAt,
      additionalInformation = SnsEventAdditionalInformation(
        applicationId = applicationId,
      ),
      personReference = SnsEventPersonReferenceCollection(
        identifiers = listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", ""),
        ),
      ),
    )

    every { asyncDomainEventWorker.domainTopic.snsClient.publish(any<PublishRequest>()) } throws RuntimeException()

    try {
      val backOffPolicy = FixedBackOffPolicy()
      backOffPolicy.backOffPeriod = AsyncDomainEventWorker.BACK_OFF_PERIOD
      val retryPolicy = SimpleRetryPolicy(AsyncDomainEventWorker.MAX_ATTEMPTS_RETRY)

      val retryTemplate = RetryTemplate()
      retryTemplate.setBackOffPolicy(backOffPolicy)
      retryTemplate.setRetryPolicy(retryPolicy)
      asyncDomainEventWorker.retryTemplate = retryTemplate
      asyncDomainEventWorker.emitEvent(snsEvent, id)
    } catch (error: RuntimeException) {
      verify(exactly = AsyncDomainEventWorker.MAX_ATTEMPTS_RETRY) {
        asyncDomainEventWorker.domainTopic.snsClient.publish(
          match<PublishRequest> { matchingPublishRequest(it, snsEvent) },
        )
      }
    }
  }

  @Test
  fun `async emit works if no exception`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { asyncDomainEventWorker.domainTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"

    val snsEvent = SnsEvent(
      eventType = "",
      version = 1,
      description = "",
      detailUrl = "detailUrl",
      occurredAt = occurredAt,
      additionalInformation = SnsEventAdditionalInformation(
        applicationId = applicationId,
      ),
      personReference = SnsEventPersonReferenceCollection(
        identifiers = listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", ""),
        ),
      ),
    )

    val publishRequest = PublishRequest.builder()
      .topicArn("")
      .message(objectMapper.writeValueAsString(snsEvent))
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
        ),
      ).build()

    every { asyncDomainEventWorker.domainTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    asyncDomainEventWorker.emitEvent(snsEvent, id)

    verify(exactly = 1) {
      asyncDomainEventWorker.domainTopic.snsClient.publish(
        match<PublishRequest> { matchingPublishRequest(it, snsEvent) },
      )
    }
  }

  @Test
  fun `sync emit happens once`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    every { syncDomainEventWorker.domainTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"

    val snsEvent = SnsEvent(
      eventType = "",
      version = 1,
      description = "",
      detailUrl = "detailUrl",
      occurredAt = occurredAt,
      additionalInformation = SnsEventAdditionalInformation(
        applicationId = applicationId,
      ),
      personReference = SnsEventPersonReferenceCollection(
        identifiers = listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", ""),
        ),
      ),
    )

    every { syncDomainEventWorker.domainTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(
      PublishResponse.builder().build(),
    )

    syncDomainEventWorker.emitEvent(snsEvent, id)

    verify(exactly = 1) {
      syncDomainEventWorker.domainTopic.snsClient.publish(
        match<PublishRequest> { matchingPublishRequest(it, snsEvent) },
      )
    }
  }

  @Test
  fun `sync emit emits event to SNS`() {
    val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
    val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
    val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
    val crn = "CRN"

    val snsEvent = SnsEvent(
      eventType = "approved-premises.application.submitted",
      version = 1,
      description = "An application has been submitted for an Approved Premises placement",
      detailUrl = "http://api/events/application-submitted/$id",
      occurredAt = occurredAt,
      additionalInformation = SnsEventAdditionalInformation(
        applicationId = applicationId,
      ),
      personReference = SnsEventPersonReferenceCollection(
        identifiers = listOf(
          SnsEventPersonReference("CRN", crn),
          SnsEventPersonReference("NOMS", ""),
        ),
      ),
    )

    every { syncDomainEventWorker.domainTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
    every { syncDomainEventWorker.domainTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(PublishResponse.builder().build())

    syncDomainEventWorker.emitEvent(snsEvent, id)

    verify(exactly = 1) {
      syncDomainEventWorker.domainTopic.snsClient.publish(
        match<PublishRequest> { matchingPublishRequest(it, snsEvent) },
      )
    }
  }

  private fun matchingPublishRequest(publishRequest: PublishRequest, snsEvent: SnsEvent): Boolean {
    val deserializedMessage = objectMapper.readValue(publishRequest.message(), SnsEvent::class.java)

    return deserializedMessage.eventType == snsEvent.eventType &&
      deserializedMessage.version == snsEvent.version &&
      deserializedMessage.description == snsEvent.description &&
      deserializedMessage.detailUrl == snsEvent.detailUrl &&
      deserializedMessage.additionalInformation.applicationId == snsEvent.additionalInformation.applicationId
  }
}
