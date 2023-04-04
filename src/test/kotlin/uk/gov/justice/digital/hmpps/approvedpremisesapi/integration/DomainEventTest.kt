package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
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
      timestamp = Instant.now(),
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

  @Test
  fun `Get Application Assessed Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/application-assessed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Application Assessed Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/application-assessed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Application Assessed Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = ApplicationAssessedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.application.assessed",
      eventDetails = ApplicationAssessedFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/application-assessed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(ApplicationAssessedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Booking Made Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/booking-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Booking Made Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/booking-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Booking Made Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = BookingMadeEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.booking.made",
      eventDetails = BookingMadeFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/booking-made/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(BookingMadeEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Person Arrivd Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/person-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Person Arrived Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/person-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Person Arrived Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonArrivedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.arrived",
      eventDetails = PersonArrivedFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/person-arrived/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(PersonArrivedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Person Not Arrived Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/person-not-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Person Not Arrived Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/person-not-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Person Not Arrived Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonNotArrivedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.not-arrived",
      eventDetails = PersonNotArrivedFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/person-not-arrived/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(PersonNotArrivedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Person Departed Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/person-departed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Person Departed Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username"
    )

    webTestClient.get()
      .uri("/events/person-departed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Person Departed Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS")
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonDepartedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.departed",
      eventDetails = PersonDepartedFactory().produce()
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/person-departed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(PersonDepartedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
