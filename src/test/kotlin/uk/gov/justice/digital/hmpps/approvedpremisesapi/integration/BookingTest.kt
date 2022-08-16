package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.time.LocalDate
import java.util.UUID

class BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Test
  fun `Get all Bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all Bookings on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get all Bookings on Premises without any Bookings returns empty list`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist() }
    }.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<Any>()
      .hasSize(0)
  }

  @Test
  fun `Get all Bookings returns OK with correct body`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersist()

    val bookings = bookingEntityFactory.configure {
      withPremises(premises)
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
    }.produceAndPersistMultiple(5)

    bookings[1].let { it.arrival = arrivalEntityFactory.configure { withBooking(it) }.produceAndPersist() }
    bookings[2].let {
      it.arrival = arrivalEntityFactory.configure { withBooking(it) }.produceAndPersist()
      it.departure = departureEntityFactory.configure {
        withBooking(it)
        withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
        withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
        withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
      }.produceAndPersist()
    }
    bookings[3].let { it.cancellation = cancellationEntityFactory.configure { withBooking(it) }.produceAndPersist() }
    bookings[4].let { it.nonArrival = nonArrivalEntityFactory.configure { withBooking(it) }.produceAndPersist() }

    val expectedJson = objectMapper.writeValueAsString(
      bookings.map {
        // TODO: Once client to Community API is in place, replace the Person with an entityFactory connected to a mock client
        bookingTransformer.transformJpaToApi(it, Person(crn = it.crn, name = "Mock Person", isActive = true))
      }
    )

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Create Booking without JWT returns 401`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersist()

    val keyWorker = keyWorkerEntityFactory.produceAndPersist()

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          expectedArrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-30"),
          keyWorkerId = keyWorker.id
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create booking on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    val keyWorker = keyWorkerEntityFactory.produceAndPersist()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          expectedArrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-30"),
          keyWorkerId = keyWorker.id
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create booking with non existent Key Worker returns Bad Request with correct body`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          expectedArrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-30"),
          keyWorkerId = UUID.randomUUID()
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "keyWorkerId",
          "errorType" to "Invalid keyWorkerId"
        )
      )
  }

  @Test
  fun `Create Booking returns OK with correct body`() {
    val premises = premisesEntityFactory.configure {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
      }
    }.produceAndPersist()

    val keyWorker = keyWorkerEntityFactory.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          expectedArrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-30"),
          keyWorkerId = keyWorker.id
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".person.crn").isEqualTo("a crn")
      .jsonPath(".person.name").isEqualTo("Mock Person")
      .jsonPath(".arrivalDate").isEqualTo("2022-08-12")
      .jsonPath(".departureDate").isEqualTo("2022-08-30")
      .jsonPath(".keyWorker.id").isEqualTo(keyWorker.id.toString())
      .jsonPath(".keyWorker.name").isEqualTo(keyWorker.name)
      .jsonPath(".status").isEqualTo("awaiting-arrival")
      .jsonPath(".arrival").isEqualTo(null)
      .jsonPath(".departure").isEqualTo(null)
      .jsonPath(".nonArrival").isEqualTo(null)
      .jsonPath(".cancellation").isEqualTo(null)
  }

  @Test
  fun `Create Arrival without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/arrivals")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Arrival on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings/e00efccb-5551-42fb-afff-2de7cb8277ff/arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create Arrival on Booking with existing Arrival returns 400`() {
    val booking = bookingEntityFactory.configure {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.configure {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.configure { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }.produceAndPersist()
          }
        }.produceAndPersist()
      }
    }.produceAndPersist()

    arrivalEntityFactory.configure { withBooking(booking) }.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".detail").isEqualTo("This Booking already has an Arrival set")
  }

  @Test
  fun `Create Arrival on Booking with expected departure before arrival date returns 400`() {
    val booking = bookingEntityFactory.configure {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.configure {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.configure {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }.produceAndPersist()
          }
        }.produceAndPersist()
      }
    }.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-16"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "expectedDepartureDate",
          "errorType" to "Cannot be before arrivalDate"
        )
      )
  }

  @Test
  fun `Create Arrival on Booking returns 200 with correct body`() {
    val booking = bookingEntityFactory.configure {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.configure {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.configure {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }.produceAndPersist()
          }
        }.produceAndPersist()
      }
    }.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = "Hello"
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".bookingId").isEqualTo(booking.id.toString())
      .jsonPath(".arrivalDate").isEqualTo("2022-08-12")
      .jsonPath(".expectedDepartureDate").isEqualTo("2022-08-14")
      .jsonPath(".notes").isEqualTo("Hello")
  }
}
