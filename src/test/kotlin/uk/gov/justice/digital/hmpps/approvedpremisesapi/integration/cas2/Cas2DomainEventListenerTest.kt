package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class Cas2DomainEventListenerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @SpykBean
  private lateinit var mockDomainEventListener: DomainEventListener

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }

  private val domainEventsClient by lazy { domainEventsTopic.snsClient }

  private fun publishMessageToTopic(eventType: String, json: String) {
    val sendMessageRequest = PublishRequest.builder()
      .topicArn(domainEventsTopic.arn)
      .message(json)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
        ),
      )
      .build()
    domainEventsClient.publish(sendMessageRequest).get()
  }

  private fun buildMessageJson(
    eventType: String = "unwanted",
    detailUrl: String = "/some/url",
    occurredAt: String = OffsetDateTime.now().toString(),
    type: String = "TYPE",
    value: String = "VALUE",
  ): String {
    return """
      {
        "eventType":"$eventType",
        "detailUrl":"$detailUrl",
        "occurredAt":"$occurredAt",
        "personReference":{"identifiers":[{"type":"$type","value":"$value"}]}
      }"
    """.trimIndent()
  }

  @Test
  fun `Start to process Allocation Changed Message on Domain Events Topic`() {
    val eventType = "offender-management.allocation.changed"

    publishMessageToTopic(eventType, buildMessageJson(eventType = eventType))
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 1) { mockDomainEventListener.processMessage(any()) }
  }

  @Test
  fun `Do not process Message that is not a required event type`() {
    val eventType = "unwanted"

    publishMessageToTopic(eventType, buildMessageJson(eventType = eventType))
    TimeUnit.MILLISECONDS.sleep(10000)
    verify(exactly = 0) { mockDomainEventListener.processMessage(any()) }
  }
}
