package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cas2ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.Cas2ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

class Cas2DomainEventTest : IntegrationTestBase() {
  @Test
  fun `Get CAS2 Application Submitted Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas2/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get CAS2 Application Submitted Event without ROLE_CAS2_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas2/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get CAS2 Application Submitted Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_CAS2_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = Cas2ApplicationSubmittedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "cas2.application.submitted",
      eventDetails = Cas2ApplicationSubmittedFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas2/application-submitted/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(Cas2ApplicationSubmittedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
