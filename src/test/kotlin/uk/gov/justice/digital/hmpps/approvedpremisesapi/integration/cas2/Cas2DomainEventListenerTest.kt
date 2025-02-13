package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException

class Cas2DomainEventListenerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @SpykBean
  private lateinit var mockCas2DomainEventListener: Cas2DomainEventListener

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }

  private val domainEventsClient by lazy { domainEventsTopic.snsClient }

  private fun publishMessageToTopic(eventType: String, json: String = "{}") {
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

  @Test
  fun `Start to process Allocation Changed Message on Domain Events Topic`() {
    val eventType = "offender-management.allocation.changed"
    publishMessageToTopic(eventType)
    verify(exactly = 1, timeout = 5000) { mockCas2DomainEventListener.processMessage(any()) }
  }

  @Test
  fun `Start to process Location Changed Message on Domain Events Topic`() {
    val eventType = "prisoner-offender-search.prisoner.updated"
    publishMessageToTopic(eventType)
    verify(exactly = 1, timeout = 5000) { mockCas2DomainEventListener.processMessage(any()) }
  }

  @Test
  fun `Do not process Message that is not a required event type`() {
    val eventType = "unwanted"
    publishMessageToTopic(eventType)
    verify(exactly = 0, timeout = 5000) { mockCas2DomainEventListener.processMessage(any()) }
  }
}
