package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID

interface DomainEventWorkerInterface {
  fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID)
}

@Component
@Primary
class ConfiguredDomainEventWorker(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) : DomainEventWorkerInterface {
  val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("domainevents not found")
  }

  override fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID) {
    SyncDomainEventWorker(this.domainTopic, this.objectMapper).emitEvent(snsEvent, domainEventId)
  }
}

class SyncDomainEventWorker(
  val domainTopic: HmppsTopic,
  val objectMapper: ObjectMapper,
) : DomainEventWorkerInterface {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID) {
    val publishRequest =
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(snsEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
          ),
        ).build()
    val publishResult = domainTopic.snsClient.publish(publishRequest).get()

    log.info(
      "Emitted SNS event (Message Id: ${publishResult.messageId()}, " +
        "Sequence Id: ${publishResult.sequenceNumber()}) for Domain Event:" +
        " $domainEventId of type: ${snsEvent.eventType}",
    )
  }
}
