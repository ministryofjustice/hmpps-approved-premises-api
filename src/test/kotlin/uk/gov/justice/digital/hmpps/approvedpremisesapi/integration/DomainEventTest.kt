package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
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
      username = "username",
    )

    webTestClient.get()
      .uri("/events/application-submitted/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Application Submitted Event with only ROLE_CAS2_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_CAS2_EVENTS"),
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = ApplicationSubmittedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.application.submitted",
      eventDetails = ApplicationSubmittedFactory().produce(),
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
      username = "username",
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = ApplicationAssessedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.application.assessed",
      eventDetails = ApplicationAssessedFactory().produce(),
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
      username = "username",
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = BookingMadeEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.booking.made",
      eventDetails = BookingMadeFactory().produce(),
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
  fun `Get Booking Cancelled Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/booking-cancelled/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Booking Cancelled Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/booking-cancelled/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Booking Cancelled Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = BookingCancelledEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.booking.cancelled",
      eventDetails = BookingCancelledFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/booking-cancelled/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(BookingCancelledEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Booking Changed Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/booking-changed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Booking Changed Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/booking-changed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Booking Changed Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = BookingChangedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.booking.changed",
      eventDetails = BookingChangedFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/booking-changed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(BookingChangedEnvelope::class.java)
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
      username = "username",
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonArrivedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.arrived",
      eventDetails = PersonArrivedFactory().produce(),
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
      username = "username",
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonNotArrivedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.not-arrived",
      eventDetails = PersonNotArrivedFactory().produce(),
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
      username = "username",
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
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PersonDepartedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.person.departed",
      eventDetails = PersonDepartedFactory().produce(),
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

  @Test
  fun `Get Booking Not Made Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/booking-not-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Booking Not Made Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/booking-not-made/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Booking Not Made Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = BookingNotMadeEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.booking.not-made",
      eventDetails = BookingNotMadeFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/booking-not-made/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(BookingNotMadeEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Application Withdrawn Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/application-withdrawn/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Application Withdrawn Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/application-withdrawn/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Application Withdrawn Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = ApplicationWithdrawnEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.application.withdrawn",
      eventDetails = ApplicationWithdrawnFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/application-withdrawn/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(ApplicationWithdrawnEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Assessment Appealed Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/assessment-appealed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Assessment Appealed Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/assessment-appealed/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Assessment Appealed Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = AssessmentAppealedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.assessment.appealed",
      eventDetails = AssessmentAppealedFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/assessment-appealed/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AssessmentAppealedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Placement Application Withdrawn Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/placement-application-withdrawn/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Placement Application Withdrawn Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/placement-application-withdrawn/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Placement Application Withdrawn Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PlacementApplicationWithdrawnEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.placement-application.withdrawn",
      eventDetails = PlacementApplicationWithdrawnFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/placement-application-withdrawn/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(PlacementApplicationWithdrawnEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  @Test
  fun `Get Placement Application Allocated Event without JWT returns 401`() {
    webTestClient.get()
      .uri("/events/placement-application-allocated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Placement Application Allocated Event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/placement-application-allocated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Placement Application Allocated Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = PlacementApplicationAllocatedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.placement-application.allocated",
      eventDetails = PlacementApplicationAllocatedFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/placement-application-allocated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(PlacementApplicationAllocatedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }

  fun `Get Assessment Allocated event without ROLE_APPROVED_PREMISES_EVENTS returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri("/events/assessment-allocated/e4b004f8-bdb2-4bf6-9958-db602be71ed3")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get Assessment Allocated Event returns 200 with correct body`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val eventId = UUID.randomUUID()

    val envelopedData = AssessmentAllocatedEnvelope(
      id = eventId,
      timestamp = Instant.now(),
      eventType = "approved-premises.assessment.allocated",
      eventDetails = AssessmentAllocatedFactory().produce(),
    )

    val event = domainEventFactory.produceAndPersist {
      withId(eventId)
      withType(DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED)
      withData(objectMapper.writeValueAsString(envelopedData))
    }

    val response = webTestClient.get()
      .uri("/events/assessment-allocated/${event.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(AssessmentAllocatedEnvelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(envelopedData)
  }
}
