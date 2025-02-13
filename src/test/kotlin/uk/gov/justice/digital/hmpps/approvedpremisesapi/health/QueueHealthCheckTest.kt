package uk.gov.justice.digital.hmpps.approvedpremisesapi.health

import com.ninjasquad.springmockk.SpykBean
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException

class QueueHealthCheckTest : IntegrationTestBase() {

  @SpykBean
  private lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  fun HmppsSqsProperties.domainEventsListenerQueueConfig() = queues["castwodomaineventslistenerqueue"]
    ?: throw MissingQueueException("castwodomaineventslistenerqueue has not been loaded from configuration properties")

  @Test
  fun `CAS 2 Domain Events queue health ok`() {
    webTestClient.get().uri("/health").exchange().expectStatus().isOk.expectBody().jsonPath("status").isEqualTo("UP")
      .jsonPath("components.castwodomaineventslistenerqueue-health.status").isEqualTo("UP")
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.queueName")
      .isEqualTo(hmppsSqsPropertiesSpy.domainEventsListenerQueueConfig().queueName)
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.dlqName")
      .isEqualTo(hmppsSqsPropertiesSpy.domainEventsListenerQueueConfig().dlqName)
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.castwodomaineventslistenerqueue-health.details.messagesOnDlq").isEqualTo(0)
  }
}
