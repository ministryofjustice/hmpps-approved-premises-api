package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2StatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationStatusUpdatedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2StatusFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Cas2DomainEventTest : IntegrationTestBase() {

  @Nested
  inner class ApplicationSubmitted {
    @Test
    fun `without JWT returns 401`() {
      webTestClient.get()
        .uri("/events/cas2/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without ROLE_CAS2_EVENTS returns 403`() {
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
    fun `with only ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
      )

      webTestClient.get()
        .uri("/events/cas2/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `returns 200 with correct body`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        roles = listOf("ROLE_CAS2_EVENTS"),
      )

      val eventId = UUID.randomUUID()

      val eventToSave = Cas2ApplicationSubmittedEvent(
        id = eventId,
        timestamp = Instant.now(),
        eventType = EventType.applicationSubmitted,
        eventDetails = Cas2ApplicationSubmittedEventDetailsFactory()
          .withReferringPrisonCode("BRI")
          .withPreferredAreas("Bradford | Leeds")
          .withHdcEligibilityDate(LocalDate.parse("2023-03-30"))
          .withConditionalReleaseDate(LocalDate.parse("2023-04-29"))
          .produce(),
      )

      val event = domainEventFactory.produceAndPersist {
        withId(eventId)
        withType(DomainEventType.CAS2_APPLICATION_SUBMITTED)
        withData(objectMapper.writeValueAsString(eventToSave))
      }

      val response = webTestClient.get()
        .uri("/events/cas2/application-submitted/${event.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(Cas2ApplicationSubmittedEvent::class.java)
        .returnResult()

      assertThat(response.responseBody).isEqualTo(eventToSave)
    }
  }

  @Nested
  inner class ApplicationStatusUpdated {
    @Test
    fun `without JWT returns 401`() {
      webTestClient.get()
        .uri("/events/cas2/application-status-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without ROLE_CAS2_EVENTS returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
      )

      webTestClient.get()
        .uri("/events/cas2/application-status-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `with only ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
      )

      webTestClient.get()
        .uri("/events/cas2/application-status-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `returns 200 with correct body`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        roles = listOf("ROLE_CAS2_EVENTS"),
      )

      val eventId = UUID.randomUUID()

      val statusDetails = listOf(
        Cas2StatusDetail("exclusionZonesAndAreas", "Exclusion zones and preferred areas"),
        Cas2StatusDetail("riskOfSeriousHarm", "Risk of serious harm"),
        Cas2StatusDetail("hdcAndCpp", "HDC licence and CPP details"),
      )

      val eventStatus = Cas2StatusFactory()
        .withStatusDetails(statusDetails)
        .produce()

      val eventDetails = Cas2ApplicationStatusUpdatedEventDetailsFactory()
        .withStatus(eventStatus)
        .produce()

      val eventToSave = Cas2ApplicationStatusUpdatedEvent(
        id = eventId,
        timestamp = Instant.now(),
        eventType = EventType.applicationStatusUpdated,
        eventDetails = eventDetails,
      )

      val event = domainEventFactory.produceAndPersist {
        withId(eventId)
        withType(DomainEventType.CAS2_APPLICATION_STATUS_UPDATED)
        withData(objectMapper.writeValueAsString(eventToSave))
      }

      val response = webTestClient.get()
        .uri("/events/cas2/application-status-updated/${event.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(Cas2ApplicationStatusUpdatedEvent::class.java)
        .returnResult()

      assertThat(response.responseBody).isEqualTo(eventToSave)
    }
  }
}
