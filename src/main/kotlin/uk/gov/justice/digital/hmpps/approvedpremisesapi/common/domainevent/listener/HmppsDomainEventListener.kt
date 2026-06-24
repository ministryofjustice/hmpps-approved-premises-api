package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.domainevent.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.InboxEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.jpa.ProcessedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.hmpps.sqs.SnsMessage
import java.time.Instant
import java.util.UUID

@ConditionalOnProperty(name = ["domain-events.listener.enabled"], havingValue = "true")
@Component
class HmppsDomainEventListener(
  private val jsonMapper: JsonMapper,
  private val inboxEventService: InboxEventService,
  private val sentryService: SentryService,
) {
  companion object {
    /**
     * Message visibility should be set according to the longest possible processing
     * time for a domain event. Given that we're writing straight into a database
     * table, this can be short
     */
    const val MESSAGE_VISIBILITY_TIMEOUT: Long = 30L

    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  @SqsListener(
    value = ["cas-domain-events-listener-queue"],
    factory = "hmppsQueueContainerFactoryProxy",
    messageVisibilitySeconds = MESSAGE_VISIBILITY_TIMEOUT.toString(),
  )
  fun processMessage(msg: String) {
    try {
      val snsMessage = jsonMapper.readValue<SnsMessage>(msg)
      val event = jsonMapper.readValue<HmppsDomainEvent>(snsMessage.message)
      inboxEventService.saveInboxEvent(
        InboxEventEntity(
          id = UUID.randomUUID(),
          eventType = event.eventType,
          eventDetailUrl = event.detailUrl,
          eventOccurredAt = event.occurredAt.toOffsetDateTime(),
          createdAt = Instant.now(),
          processedStatus = ProcessedStatus.PENDING,
          processedAt = null,
          payload = snsMessage.message,
        ),
      )
    } catch (e: Exception) {
      log.error("Exception caught in HmppsDomainEventListener", e)
      sentryService.captureException(e)
      throw e
    }
  }
}
