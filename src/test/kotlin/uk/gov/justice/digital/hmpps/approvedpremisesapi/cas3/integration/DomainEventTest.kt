package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingCancelledEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingConfirmedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3BookingProvisionallyMadeEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonArrivedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3PersonDepartedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.CAS3ReferralSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3ReferralSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

class DomainEventTest : IntegrationTestBase() {
  @Test
  fun `Get 'booking cancelled' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/booking-cancelled/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'booking cancelled' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/booking-cancelled/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'booking cancelled' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingCancelledEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingCancelled,
      eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CANCELLED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-cancelled/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingCancelledEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking cancelled' event returns 200 with correct body and empty staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingCancelledEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingCancelled,
      eventDetails = CAS3BookingCancelledEventDetailsFactory()
        .withCancelledBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CANCELLED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-cancelled/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingCancelledEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking confirmed' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/booking-confirmed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'booking confirmed' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/booking-confirmed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'booking confirmed' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingConfirmedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingConfirmed,
      eventDetails = CAS3BookingConfirmedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CONFIRMED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-confirmed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingConfirmedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking confirmed' event returns 200 with correct body and empty staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingConfirmedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingConfirmed,
      eventDetails = CAS3BookingConfirmedEventDetailsFactory()
        .withConfirmedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CONFIRMED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-confirmed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingConfirmedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking provisionally made' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/booking-provisionally-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'booking provisionally made' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/booking-provisionally-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'booking provisionally made' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingProvisionallyMadeEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingProvisionallyMade,
      eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-provisionally-made/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingProvisionallyMadeEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking provisionally made' event returns 200 with correct body and empty staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingProvisionallyMadeEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingProvisionallyMade,
      eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
        .withBookedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-provisionally-made/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingProvisionallyMadeEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking provisionally made' event returns 200 with correct body including staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingProvisionallyMadeEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingProvisionallyMade,
      eventDetails = CAS3BookingProvisionallyMadeEventDetailsFactory()
        .withBookedBy(StaffMemberFactory().produce())
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_PROVISIONALLY_MADE)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-provisionally-made/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingProvisionallyMadeEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person arrived' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/person-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'person arrived' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/person-arrived/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'person arrived' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonArrivedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personArrived,
      eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_ARRIVED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-arrived/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonArrivedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person arrived' event returns 200 with correct body and empty staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonArrivedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personArrived,
      eventDetails = CAS3PersonArrivedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_ARRIVED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-arrived/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonArrivedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person departed' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/person-departed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'person departed' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/person-departed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'person departed' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonDepartedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personDeparted,
      eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_DEPARTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-departed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonDepartedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person departed' event returns 200 with correct body without staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonDepartedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personDeparted,
      eventDetails = CAS3PersonDepartedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_DEPARTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-departed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonDepartedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'referral submitted' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/referral-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'referral submitted' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/referral-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'referral submitted' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3ReferralSubmittedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.referralSubmitted,
      eventDetails = CAS3ReferralSubmittedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_REFERRAL_SUBMITTED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/referral-submitted/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3ReferralSubmittedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person departure updated' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/person-departure-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'person departure updated' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/person-departure-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'person departure updated' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonDepartureUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personDepartureUpdated,
      eventDetails = CAS3PersonDepartedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-departure-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonDepartureUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person departure updated' event returns 200 with correct body without staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonDepartureUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personDepartureUpdated,
      eventDetails = CAS3PersonDepartedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-departure-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonDepartureUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking cancelled updated' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/booking-cancelled-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'booking cancelled updated' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/booking-cancelled-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'booking cancelled updated' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingCancelledUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingCancelledUpdated,
      eventDetails = CAS3BookingCancelledEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-cancelled-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingCancelledUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'booking cancelled updated' event returns 200 with correct body without staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3BookingCancelledUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.bookingCancelledUpdated,
      eventDetails = CAS3BookingCancelledEventDetailsFactory()
        .withCancelledBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/booking-cancelled-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3BookingCancelledUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person arrived updated' event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/cas3/person-arrived-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get 'person arrived updated' event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/cas3/person-arrived-updated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get 'person arrived updated' event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonArrivedUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personArrivedUpdated,
      eventDetails = CAS3PersonArrivedEventDetailsFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_ARRIVED_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-arrived-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonArrivedUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get 'person arrived updated' event returns 200 with correct body without staff detail`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = CAS3PersonArrivedUpdatedEvent(
      id = eventId,
      timestamp = Instant.now(),
      eventType = EventType.personArrivedUpdated,
      eventDetails = CAS3PersonArrivedEventDetailsFactory()
        .withRecordedBy(null)
        .produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.CAS3_PERSON_ARRIVED_UPDATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/cas3/person-arrived-updated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(CAS3PersonArrivedUpdatedEvent::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
