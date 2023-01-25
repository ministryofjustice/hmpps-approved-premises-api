package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApprovedPremisesBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTemporaryAccommodationBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Get a booking for an Approved Premises returns OK with the correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.approvedPremises)
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
              bookingTransformer.transformJpaToApi(booking, offenderDetails, inmateDetails, keyWorker)
            )
          )
      }
    }
  }

  @Test
  fun `Get a booking for an Temporary Accommodation Premises returns OK with the correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
          withYieldedBed { bed }
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
              bookingTransformer.transformJpaToApi(booking, offenderDetails, inmateDetails, null)
            )
          )
      }
    }
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
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Get all Bookings on Premises without any Bookings returns empty list when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
      }

      webTestClient.get()
        .uri("/premises/${premises.id}/bookings")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<Any>()
        .hasSize(0)
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Get all Bookings returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
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
            bookingTransformer.transformJpaToApi(it, offenderDetails, inmateDetails, keyWorker)
          }
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Test
  fun `Create Booking without JWT returns 401`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .bodyValue(
        NewApprovedPremisesBooking(
          crn = "a crn",
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.approvedPremises
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Approved Premises Booking returns Bad Request when no application exists for CRN`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApprovedPremisesBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.approvedPremises,
            )
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotHaveApplication")
      }
    }
  }

  @Test
  fun `Create Approved Premises Booking returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(userEntity)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          withSubmittedAt(OffsetDateTime.now())
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApprovedPremisesBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.approvedPremises,
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.keyWorker").isEqualTo(null)
          .jsonPath("$.status").isEqualTo("awaiting-arrival")
          .jsonPath("$.arrival").isEqualTo(null)
          .jsonPath("$.departure").isEqualTo(null)
          .jsonPath("$.nonArrival").isEqualTo(null)
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.serviceName").isEqualTo(ServiceName.approvedPremises.value)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
          .jsonPath("$.bed.id").doesNotHaveJsonPath()
          .jsonPath("$.bed.name").doesNotHaveJsonPath()
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewTemporaryAccommodationBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.keyWorker").isEqualTo(null)
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").isEqualTo(null)
          .jsonPath("$.departure").isEqualTo(null)
          .jsonPath("$.nonArrival").isEqualTo(null)
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.confirmation").isEqualTo(null)
          .jsonPath("$.serviceName").isEqualTo(ServiceName.temporaryAccommodation.value)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
          .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
          .jsonPath("$.bed.name").isEqualTo("test-bed")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 400 when bed does not exist on the premises`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewTemporaryAccommodationBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = UUID.randomUUID(),
            )
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }
  }

  @Test
  fun `Create Booking returns 400 when the departure date is before the arrival date`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewTemporaryAccommodationBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-09-30"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            )
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("beforeBookingArrivalDate")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when another booking for the same bed overlaps`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewTemporaryAccommodationBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            )
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val existingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        existingBooking.cancellation = cancellationEntityFactory.produceAndPersist {
          withYieldedBooking { existingBooking }
          withDate(LocalDate.parse("2022-07-01"))
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewTemporaryAccommodationBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            )
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.keyWorker").isEqualTo(null)
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").isEqualTo(null)
          .jsonPath("$.departure").isEqualTo(null)
          .jsonPath("$.nonArrival").isEqualTo(null)
          .jsonPath("$.cancellation").isEqualTo(null)
          .jsonPath("$.confirmation").isEqualTo(null)
          .jsonPath("$.serviceName").isEqualTo(ServiceName.temporaryAccommodation.value)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
          .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
          .jsonPath("$.bed.name").isEqualTo("test-bed")
      }
    }
  }

  @Test
  fun `Create Arrival without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/arrivals")
      .bodyValue(
        NewArrival(
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null,
          keyWorkerStaffCode = "123"
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Temporary Accommodation Arrival returns 409 Conflict when another booking for the same bed overlaps with the arrival and expected departure dates`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val conflictingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-06-14"))
          withDepartureDate(LocalDate.parse("2022-07-14"))
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewArrival(
              arrivalDate = LocalDate.parse("2022-06-16"),
              expectedDepartureDate = LocalDate.parse("2022-07-16"),
              notes = "Moved in late due to sickness",
              keyWorkerStaffCode = null,
            )
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Arrival on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          approvedPremisesEntityFactory.produceAndPersist {
            withQCode("QCODE")
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          }
        }
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewArrival(
            arrivalDate = LocalDate.parse("2022-08-12"),
            expectedDepartureDate = LocalDate.parse("2022-08-14"),
            notes = "Hello",
            keyWorkerStaffCode = keyWorker.code
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
        .jsonPath("$.expectedDepartureDate").isEqualTo("2022-08-14")
        .jsonPath("$.notes").isEqualTo("Hello")
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @Test
  fun `Create Arrival does not update arrival or departure date for an Approved Premises booking`() {
    `Given a User`(roles = listOf(UserRole.MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises {
            approvedPremisesEntityFactory.produceAndPersist {
              withQCode("QCODE")
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist {
                  withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
                }
              }
            }
          }
          withArrivalDate(LocalDate.parse("2022-08-10"))
          withDepartureDate(LocalDate.parse("2022-08-30"))
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewArrival(
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = keyWorker.code
            )
          )
          .exchange()
          .expectStatus()
          .isOk

        webTestClient.get()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")
      }
    }
  }

  @Test
  fun `Create Arrival updates arrival and departure date for a Temporary Accommodation booking`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withYieldedPremises { premises }
            }
          }
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withServiceName(ServiceName.temporaryAccommodation)
          withArrivalDate(LocalDate.parse("2022-08-10"))
          withDepartureDate(LocalDate.parse("2022-08-30"))
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewArrival(
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null
            )
          )
          .exchange()
          .expectStatus()
          .isOk

        webTestClient.get()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-14")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Departure on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          approvedPremisesEntityFactory.produceAndPersist {
            withQCode("QCODE")
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          }
        }
        withServiceName(ServiceName.approvedPremises)
        withArrivalDate(LocalDate.parse("2022-08-10"))
        withDepartureDate(LocalDate.parse("2022-08-30"))
      }

      val reason = departureReasonEntityFactory.produceAndPersist {
        withServiceScope("approved-premises")
      }
      val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
        withServiceScope("approved-premises")
      }
      val destinationProvider = destinationProviderEntityFactory.produceAndPersist()

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewDeparture(
            dateTime = OffsetDateTime.parse("2022-09-01T12:34:56.789Z"),
            reasonId = reason.id,
            moveOnCategoryId = moveOnCategory.id,
            destinationProviderId = destinationProvider.id,
            notes = "Hello",
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.dateTime").isEqualTo("2022-09-01T12:34:56.789Z")
        .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
        .jsonPath("$.moveOnCategory.id").isEqualTo(moveOnCategory.id.toString())
        .jsonPath("$.destinationProvider.id").isEqualTo(destinationProvider.id.toString())
        .jsonPath("$.notes").isEqualTo("Hello")
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @Test
  fun `Create Departure does not update departure date for an Approved Premises booking`() {
    `Given a User`(roles = listOf(UserRole.MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises {
            approvedPremisesEntityFactory.produceAndPersist {
              withQCode("QCODE")
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist {
                  withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
                }
              }
            }
          }
          withServiceName(ServiceName.approvedPremises)
          withArrivalDate(LocalDate.parse("2022-08-10"))
          withDepartureDate(LocalDate.parse("2022-08-30"))
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        val reason = departureReasonEntityFactory.produceAndPersist {
          withServiceScope("approved-premises")
        }
        val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
          withServiceScope("approved-premises")
        }
        val destinationProvider = destinationProviderEntityFactory.produceAndPersist()

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewDeparture(
              dateTime = OffsetDateTime.parse("2022-09-01T12:34:56.789Z"),
              reasonId = reason.id,
              moveOnCategoryId = moveOnCategory.id,
              destinationProviderId = destinationProvider.id,
              notes = "Hello",
            )
          )
          .exchange()
          .expectStatus()
          .isOk

        webTestClient.get()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")
      }
    }
  }

  @Test
  fun `Create Departure updates the departure date for a Temporary Accommodation booking`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist {
                  withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
                }
              }
            }
          }
          withServiceName(ServiceName.temporaryAccommodation)
          withArrivalDate(LocalDate.parse("2022-08-10"))
          withDepartureDate(LocalDate.parse("2022-08-30"))
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        val reason = departureReasonEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }
        val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewDeparture(
              dateTime = OffsetDateTime.parse("2022-09-01T12:34:56.789Z"),
              reasonId = reason.id,
              moveOnCategoryId = moveOnCategory.id,
              destinationProviderId = null,
              notes = "Hello",
            )
          )
          .exchange()
          .expectStatus()
          .isOk

        webTestClient.get()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.departureDate").isEqualTo("2022-09-01")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")
      }
    }
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Cancellation on Booking returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }
        }
      }

      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }

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
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
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
  fun `Create Temporary Accommodation Extension returns 409 Conflict when another booking for the same bed overlaps with the new departure date`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val conflictingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-06-14"))
          withDepartureDate(LocalDate.parse("2022-07-14"))
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewExtension(
              newDepartureDate = LocalDate.parse("2022-07-16"),
              notes = null,
            )
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
      }
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Extension on Approved Premises Booking returns OK with expected body, updates departureDate on Booking entity when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val booking = bookingEntityFactory.produceAndPersist {
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withYieldedPremises {
          approvedPremisesEntityFactory.produceAndPersist {
            withQCode("QCODE")
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }
        }
      }

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
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

      val actualBooking = bookingRepository.findByIdOrNull(booking.id)!!

      assertThat(actualBooking.departureDate).isEqualTo(LocalDate.parse("2022-08-22"))
      assertThat(actualBooking.originalDepartureDate).isEqualTo(LocalDate.parse("2022-08-20"))
    }
  }

  @Test
  fun `Create Confirmation without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/confirmations")
      .bodyValue(
        NewConfirmation(
          notes = null
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Confirmation on Approved Premises Booking returns OK with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }
        }
        withServiceName(ServiceName.temporaryAccommodation)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewConfirmation(
            notes = null
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.dateTime").value(withinSeconds(5L), OffsetDateTime::class.java)
        .jsonPath("$.notes").isEqualTo(null)
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @Test
  fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body`() {
    `Given a User` { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }
        }
        withServiceName(ServiceName.temporaryAccommodation)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewConfirmation(
            notes = null
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.dateTime").value(withinSeconds(5L), OffsetDateTime::class.java)
        .jsonPath("$.notes").isEqualTo(null)
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "MANAGER", "MATCHER" ])
  fun `Create Non Arrival on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          approvedPremisesEntityFactory.produceAndPersist {
            withQCode("QCODE")
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
          }
        }
      }

      val nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist()

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/non-arrivals")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewNonarrival(
            date = booking.arrivalDate,
            reason = nonArrivalReason.id,
            notes = "Notes"
          )
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.date").isEqualTo(booking.arrivalDate.toString())
        .jsonPath("$.reason.id").isEqualTo(nonArrivalReason.id.toString())
        .jsonPath("$.notes").isEqualTo("Notes")
    }
  }

  fun withinSeconds(seconds: Long): Matcher<OffsetDateTime> {
    val matcher: Matcher<OffsetDateTime> = object : BaseMatcher<OffsetDateTime>() {
      private val now: OffsetDateTime = OffsetDateTime.now()

      override fun describeTo(description: Description?) {
        description?.appendText("within the last $seconds seconds (now: $now)")
      }

      override fun matches(actual: Any?): Boolean {
        val actualDateTime = when (actual) {
          is String -> OffsetDateTime.parse(actual)
          is OffsetDateTime -> actual
          else -> return false
        }

        if (now.isBefore(actualDateTime)) return false

        return actualDateTime.plusSeconds(seconds).isAfter(now)
      }
    }

    return matcher
  }
}
