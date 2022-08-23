package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.time.LocalDate
import java.util.UUID

class BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Test
  fun `Get a booking for a premises without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get a booking for a premises on a non existent booking returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/213b17bd-68a8-40dc-9d91-1af58fc01f0d/bookings/f1d3a0df-546d-40a7-b80a-57502e090e45")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get a booking for a premises when a booking does not belong to that premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val otherPremises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val booking = bookingEntityFactory.produceAndPersist() {
      withPremises(otherPremises)
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings/${booking.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get a booking for a premises returns OK with the correct body`() {
    val jwt = jwtAuthHelper.createValidJwt()

    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val booking = bookingEntityFactory.produceAndPersist() {
      withPremises(premises)
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
    }

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings/${booking.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          bookingTransformer.transformJpaToApi(booking, Person(crn = booking.crn, name = "Mock Person", isActive = true))
        )
      )
  }

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
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

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
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
      withPremises(premises)
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
    }

    bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
    bookings[2].let {
      it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
      it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
      it.departure = departureEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
        withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
        withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
      }
    }
    bookings[3].let {
      it.cancellation = cancellationEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
      }
    }
    bookings[4].let {
      it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
        withBooking(it)
        withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
      }
    }

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
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

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
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

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
    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

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
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    arrivalEntityFactory.produceAndPersist { withBooking(booking) }

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
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }
      }
    }

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
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }
      }
    }

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

  @Test
  fun `Create Cancellation without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/cancellations")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = UUID.fromString("070149f6-c194-4558-a027-f67a10da7865"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Cancellation on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings/e00efccb-5551-42fb-afff-2de7cb8277ff/cancellations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = UUID.fromString("070149f6-c194-4558-a027-f67a10da7865"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create Cancellation on Booking with non-existent Cancellation Reason 400`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = UUID.fromString("31374d05-203f-45a2-a6c8-3bed24f1fa2f"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "reason",
          "errorType" to "This reason does not exist"
        )
      )
  }

  @Test
  fun `Create Cancellation on Booking with existing Cancellation returns 400`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()

    cancellationEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(cancellationReason)
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = cancellationReason.id,
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".detail").isEqualTo("This Booking already has a Cancellation set")
  }

  @Test
  fun `Create Cancellation on Booking returns OK with correct body`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewCancellation(
          date = LocalDate.parse("2022-08-17"),
          reason = cancellationReason.id,
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".bookingId").isEqualTo(booking.id.toString())
      .jsonPath(".date").isEqualTo("2022-08-17")
      .jsonPath(".notes").isEqualTo(null)
      .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
      .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
      .jsonPath(".reason.isActive").isEqualTo(true)
  }

  @Test
  fun `Create Extension without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/extensions")
      .bodyValue(
        NewExtension(
          newDepartureDate = LocalDate.parse("2022-08-20"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Extension on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings/e00efccb-5551-42fb-afff-2de7cb8277ff/extensions")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewExtension(
          newDepartureDate = LocalDate.parse("2022-08-20"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create Extension on Booking with newDepartureDate behind current departureDate of Booking returns 400`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withDepartureDate(LocalDate.parse("2022-08-20"))
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewExtension(
          newDepartureDate = LocalDate.parse("2022-08-19"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "newDepartureDate",
          "errorType" to "Must be after the Booking's current departure date (${booking.departureDate})"
        )
      )
  }

  @Test
  fun `Create Extension on Booking returns OK with expected body, updates departureDate on Booking entity`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withDepartureDate(LocalDate.parse("2022-08-20"))
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewExtension(
          newDepartureDate = LocalDate.parse("2022-08-22"),
          notes = "notes"
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".bookingId").isEqualTo(booking.id.toString())
      .jsonPath(".previousDepartureDate").isEqualTo("2022-08-20")
      .jsonPath(".newDepartureDate").isEqualTo("2022-08-22")
      .jsonPath(".notes").isEqualTo("notes")

    assertThat(bookingRepository.findByIdOrNull(booking.id)!!.departureDate).isEqualTo(LocalDate.parse("2022-08-22"))
  }

  @Test
  fun `Create Non Arrival without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/non-arrivals")
      .bodyValue(
        NewNonarrival(
          date = LocalDate.parse("2022-08-17"),
          reason = UUID.fromString("070149f6-c194-4558-a027-f67a10da7865"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Non Arrival on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings/e00efccb-5551-42fb-afff-2de7cb8277ff/non-arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewNonarrival(
          date = LocalDate.parse("2022-08-17"),
          reason = UUID.fromString("070149f6-c194-4558-a027-f67a10da7865"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Create Non Arrival on Booking with non-existent Non Arrival Reason returns 400`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withArrivalDate(LocalDate.parse("2022-08-20"))
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/non-arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewNonarrival(
          date = LocalDate.parse("2022-08-23"),
          reason = UUID.fromString("31374d05-203f-45a2-a6c8-3bed24f1fa2f"),
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".invalid-params[0]").isEqualTo(
        mapOf(
          "propertyName" to "reason",
          "errorType" to "This reason does not exist"
        )
      )
  }

  @Test
  fun `Create Non Arrival on Booking with existing Non Arrival returns 400`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

    nonArrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(nonArrivalReason)
    }

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/non-arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewNonarrival(
          date = LocalDate.parse("2022-08-17"),
          reason = nonArrivalReason.id,
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody()
      .jsonPath(".detail").isEqualTo("This Booking already has a Non Arrival set")
  }

  @Test
  fun `Create Non Arrival on Booking returns OK with correct body`() {
    val booking = bookingEntityFactory.produceAndPersist {
      withArrivalDate(LocalDate.parse("2022-08-20"))
      withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      withYieldedPremises {
        premisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }
      }
    }

    val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/non-arrivals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewNonarrival(
          date = LocalDate.parse("2022-08-23"),
          reason = nonArrivalReason.id,
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".bookingId").isEqualTo(booking.id.toString())
      .jsonPath(".date").isEqualTo("2022-08-23")
      .jsonPath(".notes").isEqualTo(null)
      .jsonPath(".reason.id").isEqualTo(nonArrivalReason.id.toString())
      .jsonPath(".reason.name").isEqualTo(nonArrivalReason.name)
      .jsonPath(".reason.isActive").isEqualTo(true)
  }
}
