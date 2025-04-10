package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SyncDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Suppress("SwallowedException")
class DomainEventWorkerTest {
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()
  private val hmppsTopicMock = mockk<HmppsTopic>()

  private val syncDomainEventWorker = SyncDomainEventWorker(
    domainTopic = hmppsTopicMock,
    objectMapper = objectMapper,
  )

  @BeforeEach
  fun clearConstructorMocks() {
    unmockkConstructor(SyncDomainEventWorker::class)
  }

  @Test
  fun `emit happens once`() {
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
  fun `emit emits event to SNS`() {
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
