package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

class BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class GetBookingForPremises {

    @Test
    fun `Get a booking for a premises without JWT returns 401`() {
      webTestClient.get()
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a booking belonging to another premises returns not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
          val premises = givenATemporaryAccommodationPremises()

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(null)
            withServiceName(ServiceName.approvedPremises)
          }

          webTestClient.get()
            .uri("/premises/${UUID.randomUUID()}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound()
        }
      }
    }

    @Test
    fun `Get a booking returns OK with the correct body when person details for a booking could not be found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR, UserRole.CAS3_REPORTER)) { _, jwt ->
        val premises = givenATemporaryAccommodationPremises()

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn("SOME-CRN")
          withServiceName(ServiceName.approvedPremises)
        }

        apDeliusContextEmptyCaseSummaryToBulkResponse("SOME-CRN")

        webTestClient.get()
          .uri("/premises/${premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              bookingTransformer.transformJpaToApi(
                booking,
                PersonInfoResult.NotFound("SOME-CRN"),
              ),
            ),
          )
      }
    }

    @Test
    fun `Get a booking for an Temporary Accommodation Premises returns OK with the correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
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
                bookingTransformer.transformJpaToApi(
                  booking,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Get a booking for a Temporary Accommodation Premises not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { probationRegion }
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
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isForbidden
        }
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
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get all Bookings on Premises without any Bookings returns empty list`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val premises = givenATemporaryAccommodationPremises(region = user.probationRegion)

      webTestClient.get()
        .uri("/premises/${premises.id}/bookings")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<Any>()
        .hasSize(0)
    }
  }

  @Test
  fun `Get all Bookings returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = givenATemporaryAccommodationPremises(region = user.probationRegion)

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
        }

        bookings[1].let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
        }
        bookings[2].let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        val expectedJson = objectMapper.writeValueAsString(
          bookings.map {
            bookingTransformer.transformJpaToApi(
              it,
              PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
            )
          },
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Test
  fun `Get all Bookings for premises returns OK with correct body when person details for a booking could not be found`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val premises = givenATemporaryAccommodationPremises(region = user.probationRegion)

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withCrn("SOME-CRN")
        withServiceName(ServiceName.temporaryAccommodation)
      }

      apDeliusContextEmptyCaseSummaryToBulkResponse("SOME-CRN")

      val expectedJson = objectMapper.writeValueAsString(
        listOf(
          bookingTransformer.transformJpaToApi(
            booking,
            PersonInfoResult.NotFound("SOME-CRN"),
          ),
        ),
      )

      webTestClient.get()
        .uri("/premises/${premises.id}/bookings")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(expectedJson)
    }
  }

  @Test
  fun `Get all Bookings for premises returns OK with correct body when inmate details for a booking could not be found`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender(mockServerErrorForPrisonApi = true) { offenderDetails, _ ->

        val premises = givenATemporaryAccommodationPremises(region = user.probationRegion)

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
        }

        val expectedJson = objectMapper.writeValueAsString(
          listOf(
            bookingTransformer.transformJpaToApi(
              booking,
              PersonInfoResult.Success.Full(
                crn = offenderDetails.otherIds.crn,
                offenderDetailSummary = offenderDetails,
                inmateDetail = null,
              ),
            ),
          ),
        )

        webTestClient.get()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Test
  fun `Get all Bookings on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val premises = givenATemporaryAccommodationPremises(region = givenAProbationRegion { })

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
        }

        bookings[1].let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
        }
        bookings[2].let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        webTestClient.get()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create Booking without JWT returns 401`() {
    val premises = givenATemporaryAccommodationPremises()

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.temporaryAccommodation,
          bedId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
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
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when bed archived date is before the arrival date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withEndDate { arrivalDate.minusDays(1) }
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bed.endDate} which overlaps with the desired dates: ${bed.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when bed archived date is exactly on the arrival date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withEndDate { arrivalDate }
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bed.endDate} which overlaps with the desired dates: ${bed.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when bed archived date is before departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withEndDate { departureDate.minusDays(1) }
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bed.endDate} which overlaps with the desired dates: ${bed.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK response when bed archived date is exactly on departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withEndDate { departureDate }
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
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
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK response when bed archived date is in future compare the departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val bed = bedEntityFactory.produceAndPersist {
          withName("test-bed")
          withEndDate { departureDate.plusDays(1) }
          withYieldedRoom {
            roomEntityFactory.produceAndPersist {
              withName("test-room")
              withYieldedPremises { premises }
            }
          }
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
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
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when overlapping booking is a non-arrival`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val conflictingBooking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withYieldedPremises { premises }
          withYieldedBed { bed }

          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-28"))
        }

        conflictingBooking.let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = assessment.id,
            ),
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
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when assessment ID is null`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              assessmentId = null,
            ),
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
          .jsonPath("$.assessmentId").isEqualTo(null)
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when NOMS number is null`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
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
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = UUID.randomUUID(),
            ),
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
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-09-30"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
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
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when another booking for the same bed has a turnaround that overlaps with the desired dates`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val existingTurnarounds = cas3TurnaroundFactory.produceAndPersistMultiple(1) {
          withWorkingDayCount(5)
          withCreatedAt(existingBooking.createdAt)
          withBooking(existingBooking)
        }

        existingBooking.turnarounds += existingTurnarounds

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-22"),
              departureDate = LocalDate.parse("2022-09-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-22 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        existingBooking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
          withYieldedBooking { existingBooking }
          withDate(LocalDate.parse("2022-07-01"))
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }.toMutableList()

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
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
  fun `Create Temporary Accommodation Booking returns 409 Conflict when a void bedspace for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val existingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 409 Conflict when a void bedspace for the same bed overlaps with the turnaround time`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
          withTurnaroundWorkingDays(2)
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

        val existingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-06-13"),
              departureDate = LocalDate.parse("2022-07-13"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
              enableTurnarounds = true,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body when only cancelled void bedspaces for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val existingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason {
            cas3VoidBedspaceReasonEntityFactory.produceAndPersist()
          }
        }

        existingLostBed.cancellation = cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
          withVoidBedspace(existingLostBed)
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
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
  fun `Create Temporary Accommodation Booking on a premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
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
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedId = bed.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create Arrival without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/arrivals")
      .bodyValue(
        NewCas3Arrival(
          type = "CAS3",
          arrivalDate = LocalDate.parse("2022-08-12"),
          expectedDepartureDate = LocalDate.parse("2022-08-14"),
          notes = null,
          keyWorkerStaffCode = "123",
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Temporary Accommodation Arrival returns 409 Conflict when another booking for the same bed overlaps with the arrival and expected departure dates`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-06-16"),
              expectedDepartureDate = LocalDate.parse("2022-07-16"),
              notes = "Moved in late due to sickness",
              keyWorkerStaffCode = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Arrival returns 409 Conflict when a void bedspace for the same bed overlaps with the arrival and expected departure dates`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
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

        val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-06-16"),
              expectedDepartureDate = LocalDate.parse("2022-07-16"),
              notes = "Moved in late due to sickness",
              keyWorkerStaffCode = null,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
      }
    }
  }

  @Test
  fun `Create Arrival updates arrival and departure date for a Temporary Accommodation booking`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
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
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null,
            ),
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

  @Test
  fun `Create Arrival and departure date for a Temporary Accommodation booking and emit arrival domain event for new arrival`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val premises = givenATemporaryAccommodationPremises(region = userEntity.probationRegion)

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
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null,
            ),
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

        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_PERSON_ARRIVED,
          "Someone has arrived at a Transitional Accommodation premises for their booking",
          "http://api/events/cas3/person-arrived",
        )
      }
    }
  }

  @Test
  fun `Create Arrival updates arrival for a Temporary Accommodation booking with existing arrival and updated domain event send`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
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
        booking.let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(2) { withBooking(it) }.toMutableList()
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null,
            ),
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

        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_PERSON_ARRIVED_UPDATED,
          "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
          "http://api/events/cas3/person-arrived-updated",
        )
      }
    }
  }

  @Test
  fun `Create Arrival for a Temporary Accommodation booking on a premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
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
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create Arrival for a Temporary Accommodation booking on a premises that does not exist returns 404 Not Found`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
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

        val premisesId = UUID.randomUUID()

        webTestClient.post()
          .uri("/premises/$premisesId/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCas3Arrival(
              type = "CAS3",
              arrivalDate = LocalDate.parse("2022-08-12"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
      }
    }
  }

  @Nested
  inner class CreateDeparture {
    @ParameterizedTest
    @CsvSource("/premises", "cas3/premises")
    fun `Create Departure updates the departure date for a Temporary Accommodation booking`(baseUrl: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(givenATemporaryAccommodationPremises(region = userEntity.probationRegion))
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
            .uri("$baseUrl/${booking.premises.id}/bookings/${booking.id}/departures")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                destinationProviderId = null,
                notes = "Hello",
              ),
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
    fun `Create Departure on Temporary Accommodation Booking when a departure already exists returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises {
              temporaryAccommodationPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { userEntity.probationRegion }
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

          val departure = departureEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(reason)
            withMoveOnCategory(moveOnCategory)
          }
          booking.departures = mutableListOf(departure)

          webTestClient.post()
            .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                destinationProviderId = null,
                notes = "Corrected date",
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.dateTime").isEqualTo("2022-09-01T12:34:56.789Z")
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.moveOnCategory.id").isEqualTo(moveOnCategory.id.toString())
            .jsonPath("$.destinationProvider").isEqualTo(null)
            .jsonPath("$.notes").isEqualTo("Corrected date")
            .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "cas3/premises")
    fun `Create Departure for a Temporary Accommodation booking on a premises that's not in the user's region returns 403 Forbidden`(baseUrl: String) {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises {
              temporaryAccommodationPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { userEntity.probationRegion }
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
            .uri("$baseUrl/${booking.premises.id}/bookings/${booking.id}/departures")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                destinationProviderId = null,
                notes = "Hello",
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @ParameterizedTest
    @CsvSource("/premises", "cas3/premises")
    fun `Create Departure for a Temporary Accommodation booking on a premises that does not exist returns 404 Not Found`(baseUrl: String) {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val booking = bookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises {
              temporaryAccommodationPremisesEntityFactory.produceAndPersist {
                withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                withYieldedProbationRegion { userEntity.probationRegion }
              }
            }
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.now().plusDays(5))
            withDepartureDate(LocalDate.now().plusDays(63))
            withCreatedAt(OffsetDateTime.now())
          }

          val reason = departureReasonEntityFactory.produceAndPersist {
            withServiceScope("temporary-accommodation")
          }
          val moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
            withServiceScope("temporary-accommodation")
          }

          val premisesId = UUID.randomUUID()

          webTestClient.post()
            .uri("$baseUrl/$premisesId/bookings/${booking.id}/departures")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewDeparture(
                dateTime = Instant.now().plus(10, ChronoUnit.DAYS),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                destinationProviderId = null,
                notes = "Some notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
        }
      }
    }
  }

  @Test
  fun `Create Cancellation on CAS3 Booking on a premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { probationRegion }
          }
        }
      }

      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewCancellation(
            date = LocalDate.parse("2022-08-17"),
            reason = cancellationReason.id,
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Create Cancellation on CAS3 Booking on a premises that does not exist returns 404 Not Found`() {
    givenAUser { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { userEntity.probationRegion }
          }
        }
      }

      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }

      val premisesId = UUID.randomUUID()

      webTestClient.post()
        .uri("/premises/$premisesId/bookings/${booking.id}/cancellations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewCancellation(
            date = LocalDate.parse("2022-08-17"),
            reason = cancellationReason.id,
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
    }
  }

  @Test
  fun `Create Cancellation on CAS3 Booking when a cancellation already exists returns OK with correct body and send cancelled-updated event`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }
        }
      }

      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }

      val cancellation = cancellationEntityFactory.produceAndPersist {
        withBooking(booking)
        withReason(cancellationReason)
      }
      booking.cancellations = mutableListOf(cancellation)

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewCancellation(
            date = LocalDate.parse("2022-08-18"),
            reason = cancellationReason.id,
            notes = "Corrected date",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath(".bookingId").isEqualTo(booking.id.toString())
        .jsonPath(".date").isEqualTo("2022-08-18")
        .jsonPath(".notes").isEqualTo("Corrected date")
        .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
        .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
        .jsonPath(".reason.isActive").isEqualTo(true)
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

      assertPublishedSNSEvent(
        booking,
        DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
        "A cancelled booking for a Transitional Accommodation premises has been updated",
        "http://api/events/cas3/booking-cancelled-updated",
      )
    }
  }

  @Test
  fun `Create Cancellation on CAS3 Booking when a no cancellation exists returns OK with correct body and send cancelled event`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withProbationRegion(userEntity.probationRegion)
          }
        }
      }

      val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
        withServiceScope("*")
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewCancellation(
            date = LocalDate.parse("2022-08-18"),
            reason = cancellationReason.id,
            notes = "Corrected date",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath(".bookingId").isEqualTo(booking.id.toString())
        .jsonPath(".date").isEqualTo("2022-08-18")
        .jsonPath(".notes").isEqualTo("Corrected date")
        .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
        .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
        .jsonPath(".reason.isActive").isEqualTo(true)
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

      assertPublishedSNSEvent(
        booking,
        DomainEventType.CAS3_BOOKING_CANCELLED,
        "A booking for a Transitional Accommodation premises has been cancelled",
        "http://api/events/cas3/booking-cancelled",
      )
    }
  }

  @Test
  fun `Create Cancellation on Temporary Accommodation Booking when a cancellation already exists returns OK with correct body and move assessment to ready-to-place state`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withProbationRegion(userEntity.probationRegion)
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withCompletedAt(OffsetDateTime.now())
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withProbationRegion(userEntity.probationRegion)
            }
          }
          withApplication(application)
        }

        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }

        val cancellation = cancellationEntityFactory.produceAndPersist {
          withBooking(booking)
          withReason(cancellationReason)
        }
        booking.cancellations = mutableListOf(cancellation)

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCancellation(
              date = LocalDate.parse("2022-08-18"),
              reason = cancellationReason.id,
              notes = "Corrected date",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".bookingId").isEqualTo(booking.id.toString())
          .jsonPath(".date").isEqualTo("2022-08-18")
          .jsonPath(".notes").isEqualTo("Corrected date")
          .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
          .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
          "A cancelled booking for a Transitional Accommodation premises has been updated",
          "http://api/events/cas3/booking-cancelled-updated",
        )

        assertCAS3AssessmentIsReadyToPlace(assessment)
      }
    }
  }

  @Test
  fun `Create Cancellation on Temporary Accommodation Booking returns OK and make move assessment to ready-to-place state when accept assessment fail with forbidden exception`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withProbationRegion(userEntity.probationRegion)
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withReallocatedAt(OffsetDateTime.now())
          withCompletedAt(OffsetDateTime.now())
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withProbationRegion(userEntity.probationRegion)
            }
          }
          withApplication(application)
        }

        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }

        val cancellation = cancellationEntityFactory.produceAndPersist {
          withBooking(booking)
          withReason(cancellationReason)
        }
        booking.cancellations = mutableListOf(cancellation)

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewCancellation(
              date = LocalDate.parse("2022-08-18"),
              reason = cancellationReason.id,
              notes = "Corrected date",
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".bookingId").isEqualTo(booking.id.toString())
          .jsonPath(".date").isEqualTo("2022-08-18")
          .jsonPath(".notes").isEqualTo("Corrected date")
          .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
          .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
          "A cancelled booking for a Transitional Accommodation premises has been updated",
          "http://api/events/cas3/booking-cancelled-updated",
        )

        assertCAS3AssessmentIsClosed(assessment)
      }
    }
  }

  @Nested
  inner class CreateExtensionCAS3Only {

    @Test
    fun `Create Extension without JWT returns 401`() {
      webTestClient.post()
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/extensions")
        .bodyValue(
          NewExtension(
            newDepartureDate = LocalDate.parse("2022-08-20"),
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create CAS3 Extension returns 409 Conflict when another booking for the same bed overlaps with the new departure date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
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

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-16"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @Test
    fun `Create CAS3 Extension returns 409 Conflict when another booking for the same bed overlaps with the updated booking's turnaround time`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
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
            withArrivalDate(LocalDate.parse("2022-06-12"))
            withDepartureDate(LocalDate.parse("2022-07-12"))
          }

          val turnarounds = cas3TurnaroundFactory.produceAndPersistMultiple(1) {
            withWorkingDayCount(2)
            withCreatedAt(booking.createdAt)
            withBooking(booking)
          }

          booking.turnarounds += turnarounds

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-13"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @Test
    fun `Create CAS3 Extension returns 409 Conflict when a void bedspace for the same bed overlaps with the new departure date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
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

          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-16"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create CAS3 Extension returns 409 Conflict when a void bedspace for the same bed overlaps with the updated booking's turnaround time`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
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

          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withArrivalDate(LocalDate.parse("2022-06-12"))
            withDepartureDate(LocalDate.parse("2022-07-12"))
          }

          val turnarounds = cas3TurnaroundFactory.produceAndPersistMultiple(1) {
            withWorkingDayCount(2)
            withCreatedAt(booking.createdAt)
            withBooking(booking)
          }

          booking.turnarounds += turnarounds

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-13"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody()
            .jsonPath("title").isEqualTo("Conflict")
            .jsonPath("status").isEqualTo(409)
            .jsonPath("detail")
            .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create CAS3 Extension returns OK with expected body, updates departureDate on Booking entity when user has one of roles CAS3_ASSESSOR`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            userEntity.probationRegion
          }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withArrivalDate(LocalDate.parse("2022-08-18"))
          withDepartureDate(LocalDate.parse("2022-08-20"))
          withPremises(premises)
          withBed(bed)
        }

        creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-08-22")
      }
    }

    @Test
    fun `Create CAS3 Extension returns 403 Forbidden for a premises that's not in the user's region`() {
      givenAUser { _, jwt ->
        val booking = bookingEntityFactory.produceAndPersist {
          withDepartureDate(LocalDate.parse("2022-08-20"))
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion { probationRegion }
            }
          }
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewExtension(
              newDepartureDate = LocalDate.parse("2022-08-22"),
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Test
  fun `Create Confirmation without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/confirmations")
      .bodyValue(
        NewConfirmation(
          notes = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              userEntity.probationRegion
            }
          }
        }
        withServiceName(ServiceName.temporaryAccommodation)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewConfirmation(
            notes = null,
          ),
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
  fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body and close associated referral`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                userEntity.probationRegion
              }
            }
          }
          withApplication(application)
          withServiceName(ServiceName.temporaryAccommodation)
        }

        assertCAS3AssessmentIsNotClosed(assessment)

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(
            NewConfirmation(
              notes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
          .jsonPath("$.dateTime").value(withinSeconds(5L), OffsetDateTime::class.java)
          .jsonPath("$.notes").isEqualTo(null)
          .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

        assertCAS3AssessmentIsClosed(assessment)
      }
    }
  }

  @Test
  fun `Create Confirmation on Temporary Accommodation Booking for a premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { probationRegion }
          }
        }
        withServiceName(ServiceName.temporaryAccommodation)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewConfirmation(
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Create Confirmation on Temporary Accommodation Booking for a premises that does not exist returns 404 Not Found`() {
    givenAUser { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { probationRegion }
          }
        }
        withServiceName(ServiceName.temporaryAccommodation)
      }

      val premisesId = UUID.randomUUID()

      webTestClient.post()
        .uri("/premises/$premisesId/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .bodyValue(
          NewConfirmation(
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
    }
  }

  @Test
  fun `Successfully send updated departure date events when create date-change is invoked for existing booking with departure detail`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val now = LocalDateTime.now()
        val notes = "Some notes about the departure"
        val premises = createTemporaryAccommodationPremises(userEntity)
        val booking = createTemporaryAccommodationBooking(premises, offenderDetails)
        val reasonEntity = departureReasonEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }
        val moveOnCategoryEntity = moveOnCategoryEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }
        departureEntityFactory.produceAndPersist {
          withBooking(booking)
          withDateTime(now.atOffset(ZoneOffset.UTC))
          withReason(reasonEntity)
          withMoveOnCategory(moveOnCategoryEntity)
          withNotes(notes)
        }

        callPersonDepartureDateAPIAndAssertDepartureDetail(
          "/premises/${premises.id}/bookings/${booking.id}/departures",
          buildNewDeparture(now, reasonEntity, moveOnCategoryEntity, notes),
          booking,
          now,
          jwt,
        )

        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED,
          "Person has updated departure date of Transitional Accommodation premises",
          "http://api/events/cas3/person-departure-updated",
        )
      }
    }
  }

  @Test
  fun `Successfully send departed events when create departure is invoked for existing booking without departure detail`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val now = LocalDateTime.now()
        val notes = "Some notes about the departure"
        val premises = createTemporaryAccommodationPremises(userEntity)
        val booking = createTemporaryAccommodationBooking(premises, offenderDetails)
        val reasonEntity = departureReasonEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }
        val moveOnCategoryEntity = moveOnCategoryEntityFactory.produceAndPersist {
          withServiceScope("temporary-accommodation")
        }

        callPersonDepartureDateAPIAndAssertDepartureDetail(
          "/premises/${premises.id}/bookings/${booking.id}/departures",
          buildNewDeparture(now, reasonEntity, moveOnCategoryEntity, notes),
          booking,
          now,
          jwt,
        )
        assertPublishedSNSEvent(
          booking,
          DomainEventType.CAS3_PERSON_DEPARTED,
          "Someone has left a Transitional Accommodation premises",
          "http://api/events/cas3/person-departed",
        )
      }
    }
  }

  private fun callPersonDepartureDateAPIAndAssertDepartureDetail(
    url: String,
    requestBody: NewDeparture,
    booking: BookingEntity,
    now: LocalDateTime,
    jwt: String,
  ) {
    webTestClient.post()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(requestBody)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.id").isNotEmpty
      .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
      .jsonPath("$.notes").isEqualTo("Some notes about the departure")
      .jsonPath("$.dateTime").isEqualTo(now.atOffset(ZoneOffset.UTC).toString())
      .jsonPath("$.reason").isNotEmpty
      .jsonPath("$.moveOnCategory").isNotEmpty
  }

  private fun buildNewDeparture(
    now: LocalDateTime,
    reasonEntity: DepartureReasonEntity,
    moveOnCategoryEntity: MoveOnCategoryEntity,
    notes: String,
  ) = NewDeparture(
    dateTime = now.toInstant(ZoneOffset.UTC),
    reasonId = reasonEntity.id,
    moveOnCategoryId = moveOnCategoryEntity.id,
    notes = notes,
    destinationProviderId = null,
  )

  private fun createTemporaryAccommodationPremises(userEntity: UserEntity) = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withProbationRegion(userEntity.probationRegion)
    withYieldedLocalAuthorityArea {
      localAuthorityEntityFactory.produceAndPersist()
    }
  }

  private fun createTemporaryAccommodationBooking(
    premises: TemporaryAccommodationPremisesEntity,
    offenderDetails: OffenderDetailSummary,
  ): BookingEntity {
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }
    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(bed.room.premises)
      withCrn(offenderDetails.otherIds.crn)
      withBed(bed)
      withServiceName(ServiceName.temporaryAccommodation)
    }
    return booking
  }

  private fun creatingNewExtensionReturnsCorrectly(booking: BookingEntity, jwt: String, newDate: String) {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .bodyValue(
        NewExtension(
          newDepartureDate = LocalDate.parse(newDate),
          notes = "notes",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath(".bookingId").isEqualTo(booking.id.toString())
      .jsonPath(".previousDepartureDate").isEqualTo(booking.departureDate.toString())
      .jsonPath(".newDepartureDate").isEqualTo(newDate)
      .jsonPath(".notes").isEqualTo("notes")
      .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)

    val actualBooking = bookingRepository.findByIdOrNull(booking.id)!!

    assertThat(actualBooking.departureDate).isEqualTo(LocalDate.parse(newDate))
    assertThat(actualBooking.originalDepartureDate).isEqualTo(booking.departureDate)
  }

  private fun assertPublishedSNSEvent(
    booking: BookingEntity,
    eventType: DomainEventType,
    eventDescription: String,
    detailUrl: String,
  ) {
    val emittedMessage = snsDomainEventListener.blockForMessage(eventType)

    assertThat(emittedMessage.description).isEqualTo(eventDescription)
    assertThat(emittedMessage.detailUrl).matches("$detailUrl/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
    assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
      SnsEventPersonReference("CRN", booking.crn),
      SnsEventPersonReference("NOMS", booking.nomsNumber!!),
    )
  }

  private fun assertCAS3AssessmentIsNotClosed(assessment: TemporaryAccommodationAssessmentEntity) {
    val temporaryAccommodationAssessmentEntity =
      temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)

    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNull()
  }

  private fun assertCAS3AssessmentIsReadyToPlace(assessment: TemporaryAccommodationAssessmentEntity) {
    val temporaryAccommodationAssessmentEntity =
      temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)

    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNull()
    assertThat(temporaryAccommodationAssessmentEntity!!.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(temporaryAccommodationAssessmentEntity!!.submittedAt).isNotNull()
  }

  private fun assertCAS3AssessmentIsClosed(assessment: TemporaryAccommodationAssessmentEntity) {
    val temporaryAccommodationAssessmentEntity =
      temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)

    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNotNull()
  }
}
