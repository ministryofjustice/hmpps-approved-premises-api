package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.OffsetDateTime
import java.util.UUID

class DomainEventTest : IntegrationTestBase() {
  @Test
  fun `Get Application Submitted Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Application Submitted Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Application Submitted Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = ApplicationSubmittedEnvelope(
      id = eventId,
      timestamp = OffsetDateTime.now(),
      eventType = "approved-premises.application.submitted",
      eventDetails = ApplicationSubmittedFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/application-submitted/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(ApplicationSubmittedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
