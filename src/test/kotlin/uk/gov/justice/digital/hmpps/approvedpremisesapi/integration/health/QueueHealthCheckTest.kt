package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class QueueHealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Inbound queue health ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.inboundqueue-health.status").isEqualTo("UP")
      .jsonPath("components.inboundqueue-health.details.queueName").isEqualTo(hmppsSqsPropertiesSpy.inboundQueueConfig().queueName)
      .jsonPath("components.inboundqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.inboundqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.inboundqueue-health.details.dlqName").isEqualTo(hmppsSqsPropertiesSpy.inboundQueueConfig().dlqName)
      .jsonPath("components.inboundqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.inboundqueue-health.details.messagesOnDlq").isEqualTo(0)
  }
}
