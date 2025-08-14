package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsNull.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3ApplicationAndAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremiseBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremisesAndBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.withConflictMessage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@SuppressWarnings("LargeClass")
class Cas3v2BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: Cas3BookingTransformer

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
  }

  @Nested
  inner class GetBooking {

    @Test
    fun `Get a booking without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/v2/bookings/${UUID.randomUUID()}")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a non-existent booking returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        webTestClient.get()
          .uri("/cas3/v2/bookings/${UUID.randomUUID()}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get a booking returns OK with the correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val premises = givenACas3Premises(
            probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            },
          )
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(
              cas3BedspaceEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
            withCrn(offenderDetails.otherIds.crn)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
  }

  @Nested
  inner class GetBookingForPremises {

    @Test
    fun `Get a booking for a premises without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings/${UUID.randomUUID()}")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a booking for a premises returns 403 Forbidden when not a CAS3_ASSESSOR`() {
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withCrn(offenderDetails.otherIds.crn)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Get a booking for a premises not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val premises = givenACas3Premises(
            probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(probationRegion)
            },
          )

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(
              cas3BedspaceEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
            withCrn(offenderDetails.otherIds.crn)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Get a booking belonging to another premises returns not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withCrn(offenderDetails.otherIds.crn)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isBadRequest
        }
      }
    }

    @Test
    fun `Get a booking for a premises returns OK with the correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withCrn(offenderDetails.otherIds.crn)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
    fun `Get a booking returns OK with the correct body when person details for a booking could not be found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withCrn("SOME-CRN")
          withServiceName(ServiceName.temporaryAccommodation)
        }

        apDeliusContextEmptyCaseSummaryToBulkResponse("SOME-CRN")

        webTestClient.get()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
    fun `Get a booking for a premises returns OK with the correct body when the NOMS number is null`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withCrn(offenderDetails.otherIds.crn)
            withNomsNumber(null)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                bookingTransformer.transformJpaToApi(
                  booking,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, null),
                ),
              ),
            )
        }
      }
    }
  }

  @Test
  fun `Get all Bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all Bookings on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings")
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get all Bookings when user has the CAS3_ASSESSOR role but user does not have same probation region as premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      val premises = givenACas3Premises(
        probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegionEntityFactory.produceAndPersist())
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/premises/${premises.id}/bookings")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get all Bookings on Premises when user does not have the CAS3_ASSESSOR role and user has same probation region as premises`() {
    givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, jwt ->
      val premises = givenACas3Premises(
        probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/premises/${premises.id}/bookings")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get all Bookings on Premises without any Bookings returns empty list when user has the CAS3_ASSESSOR role and user has same probation region as premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      val premises = givenACas3Premises(
        probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        },
      )

      webTestClient.get()
        .uri("/cas3/v2/premises/${premises.id}/bookings")
        .headers(buildTemporaryAccommodationHeaders(jwt))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<Any>()
        .hasSize(0)
    }
  }

  @Test
  fun `Get all Bookings returns OK with correct body when user has CAS3_ASSESSOR role`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val bookings = cas3BookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
          withBedspace(bedspace)
        }

        bookings[1].let {
          it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
        }
        bookings[2].let {
          it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.extensions = cas3ExtensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = cas3DepartureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
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
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
      val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
      val booking = cas3BookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withCrn("SOME-CRN")
        withServiceName(ServiceName.temporaryAccommodation)
        withBedspace(bedspace)
      }

      val expectedJson = objectMapper.writeValueAsString(
        listOf(
          bookingTransformer.transformJpaToApi(
            booking,
            PersonInfoResult.NotFound("SOME-CRN"),
          ),
        ),
      )

      apDeliusContextEmptyCaseSummaryToBulkResponse("SOME-CRN")

      webTestClient.get()
        .uri("/cas3/v2/premises/${premises.id}/bookings")
        .headers(buildTemporaryAccommodationHeaders(jwt))
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
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
          withBedspace(bedspace)
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
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
    val cas3Premises = givenACas3Premises()
    webTestClient.post()
      .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
      .bodyValue(
        Cas3NewBooking(
          crn = "a crn",
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.temporaryAccommodation,
          bedspaceId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create Booking returns OK with correct body`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate,
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when bedspace end-date is before the arrival date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(cas3Premises)
          withEndDate(arrivalDate.minusDays(1))
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when bedspace end-date is exactly on the arrival date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(cas3Premises)
          withEndDate(arrivalDate)
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when bedspace end-date is before departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(cas3Premises)
          withEndDate(departureDate.minusDays(1))
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK response when bedspace end-date is exactly on departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(cas3Premises)
          withStartDate(arrivalDate.minusDays(1))
          withEndDate(departureDate)
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK response when bedspace end date is after the departure date`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(cas3Premises)
          withStartDate(arrivalDate.minusDays(1))
          withEndDate(departureDate.plusDays(1))
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = arrivalDate,
              departureDate = departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when overlapping booking is a non-arrival`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-07-15")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        val conflictingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(arrivalDate)
          withDepartureDate(LocalDate.parse("2022-08-28"))
        }

        conflictingBooking.let {
          it.nonArrival = cas3NonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo("${assessment.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when assessment ID is null`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate,
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo(assessment.id)
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when NOMS number is null`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate,
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-12")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
          .jsonPath("$.assessmentId").isEqualTo(assessment.id)
      }
    }
  }

  @Test
  fun `Create Booking returns 400 when bedspace does not exist on the premises`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-12"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = UUID.randomUUID(),
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
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-09-30")
        val departureDate = LocalDate.parse("2022-08-30")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate,
              departureDate,
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
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
  fun `Create Booking returns 409 Conflict when another booking for the same bedspace overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when another booking for the same bedspace has a turnaround that overlaps with the desired dates`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(LocalDate.parse("2022-07-15"))
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        val existingTurnarounds = cas3v2TurnaroundFactory.produceAndPersistMultiple(1) {
          withWorkingDayCount(5)
          withCreatedAt(existingBooking.createdAt)
          withBooking(existingBooking)
        }

        existingBooking.turnarounds += existingTurnarounds

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-22"),
              departureDate = LocalDate.parse("2022-09-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("A Booking already exists for dates from 2022-07-15 to 2022-08-22 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-07-15")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

        val existingBooking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn("CRN123")
          withPremises(premises)
          withBedspace(bedspace)
          withArrivalDate(arrivalDate)
          withDepartureDate(LocalDate.parse("2022-08-15"))
        }

        existingBooking.cancellations = cas3CancellationEntityFactory.produceAndPersistMultiple(1) {
          withYieldedBooking { existingBooking }
          withDate(LocalDate.parse("2022-07-01"))
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }.toMutableList()

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when a void bedspace for the same bed overlaps`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        val existingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .withConflictMessage("A Void Bedspace already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when only cancelled void bedspaces for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-07-15")
        val (premises, bedspace) = givenCas3PremisesAndBedspace(user, startDate = arrivalDate.minusDays(1))
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          withCancellationDate(OffsetDateTime.of(LocalDate.parse("2022-07-01"), LocalTime.NOON, ZoneOffset.UTC))
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate = LocalDate.parse("2022-08-01"),
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.person.crn").isEqualTo(offenderDetails.otherIds.crn)
          .jsonPath("$.person.name").isEqualTo("${offenderDetails.firstName} ${offenderDetails.surname}")
          .jsonPath("$.arrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.departureDate").isEqualTo("2022-08-30")
          .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-01")
          .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
          .jsonPath("$.status").isEqualTo("provisional")
          .jsonPath("$.arrival").value(nullValue())
          .jsonPath("$.departure").value(nullValue())
          .jsonPath("$.nonArrival").value(nullValue())
          .jsonPath("$.cancellation").value(nullValue())
          .jsonPath("$.confirmation").value(nullValue())
          .jsonPath("$.createdAt").value(withinSeconds(5L))
          .jsonPath("$.bedspace.id").isEqualTo(bedspace.id.toString())
          .jsonPath("$.bedspace.reference").isEqualTo("test-bed")
      }
    }
  }

  @Test
  fun `Create Booking on a premises that's not in the user's region returns 403 Forbidden`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val premises = givenACas3Premises()
        val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
          withReference("test-bed")
          withPremises(premises)
          withStartDate(arrivalDate.minusDays(1))
        }
        val (_, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)
        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            Cas3NewBooking(
              crn = offenderDetails.otherIds.crn,
              arrivalDate,
              departureDate = LocalDate.parse("2022-08-30"),
              serviceName = ServiceName.temporaryAccommodation,
              bedspaceId = bedspace.id,
              assessmentId = assessment.id,
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class CreateArrival {
    @Test
    fun `Create Arrival without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings/${UUID.randomUUID()}/arrivals")
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
    fun `Create Arrival returns 409 Conflict when another booking for the same bedspace overlaps with the arrival and expected departure dates`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val conflictingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withYieldedPremises { premises }
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
            .withConflictMessage("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @Test
    fun `Create Arrival returns 409 Conflict when a void bedspace for the same bed overlaps with the arrival and expected departure dates`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
            .withConflictMessage("A Void Bedspace already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create Arrival is successful and updates arrival date and departure date on booking and emits arrival domain event for new arrival`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val newArrivalDate = LocalDate.now().minusDays(10)
          val newExpectedDepartureDate = newArrivalDate.plusDays(2)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = newArrivalDate,
                expectedDepartureDate = newExpectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated

          webTestClient.get()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(newArrivalDate)
            .jsonPath("$.departureDate").isEqualTo(newExpectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_PERSON_ARRIVED,
            eventDescription = "Someone has arrived at a Transitional Accommodation premises for their booking",
            detailUrl = "http://api/events/cas3/person-arrived",
          )
        }
      }
    }

    @Test
    fun `Create Arrival and departure date for a booking and emit arrival domain event for new arrival`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val newArrivalDate = LocalDate.now().minusDays(10)
          val newExpectedDepartureDate = newArrivalDate.plusDays(2)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = newArrivalDate,
                expectedDepartureDate = newExpectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated

          webTestClient.get()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(newArrivalDate)
            .jsonPath("$.departureDate").isEqualTo(newExpectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_PERSON_ARRIVED,
            "Someone has arrived at a Transitional Accommodation premises for their booking",
            "http://api/events/cas3/person-arrived",
          )
        }
      }
    }

    @Test
    fun `Create Arrival updates arrival for a booking with existing arrival and updated domain event send`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }
          booking.let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(2) {
              withBooking(it)
            }.toMutableList()
          }

          val newArrivalDate = LocalDate.now().minusDays(14)
          val newExpectedDepartureDate = newArrivalDate.plusDays(2)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = newArrivalDate,
                expectedDepartureDate = newExpectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated

          webTestClient.get()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo(newArrivalDate)
            .jsonPath("$.departureDate").isEqualTo(newExpectedDepartureDate)
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_PERSON_ARRIVED_UPDATED,
            "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
            "http://api/events/cas3/person-arrived-updated",
          )
        }
      }
    }

    @Test
    fun `Create Arrival for a booking on a premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val premises = givenACas3Premises()
          val bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(premises)
          }
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
    fun `Create Arrival for a booking on a premises that does not exist returns 404 Not Found`() {
      givenAUser { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withYieldedPremises { premises }
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }

          val notFoundPremises = UUID.randomUUID()

          webTestClient.post()
            .uri("/cas3/v2/premises/$notFoundPremises/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
            .jsonPath("$.detail").isEqualTo("No Premises with an ID of $notFoundPremises could be found")
        }
      }
    }

    @Test
    fun `Create Arrival returns field validation error when the arrival date is more than 14 days in the past`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withServiceName(ServiceName.temporaryAccommodation)
            withArrivalDate(LocalDate.parse("2022-08-10"))
            withDepartureDate(LocalDate.parse("2022-08-30"))
            withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
          }
          booking.let {
            it.arrivals = cas3ArrivalEntityFactory.produceAndPersistMultiple(2) {
              withBooking(it)
            }.toMutableList()
          }

          val newArrivalDate = LocalDate.now().minusDays(15)
          val newExpectedDepartureDate = newArrivalDate.plusDays(2)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCas3Arrival(
                type = "CAS3",
                arrivalDate = newArrivalDate,
                expectedDepartureDate = newExpectedDepartureDate,
                notes = "Hello",
                keyWorkerStaffCode = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.arrivalDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("arrivalAfterLatestDate")
        }
      }
    }
  }

  @Nested
  inner class CreateCancellation {

    @Test
    fun `Create Cancellation on CAS3 Booking on a premises that is not in the user's region returns 403 Forbidden`() {
      givenAUser { userEntity, jwt ->
        val premises = givenACas3Premises()
        val bedspace = givenCas3PremiseBedspace(premises)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
        }
        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }
        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/cancellations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
    fun `Create Cancellation on CAS3 Booking for a premises that does not exist returns 404 Not Found`() {
      givenAUser { userEntity, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
        }
        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }

        val notFoundPremisesId = UUID.randomUUID()
        webTestClient.post()
          .uri("/cas3/v2/premises/$notFoundPremisesId/bookings/${booking.id}/cancellations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $notFoundPremisesId could be found")
      }
    }

    @Test
    fun `Create Cancellation on CAS3 Booking when a cancellation already exists returns OK with correct body and send cancelled-updated event`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
        }
        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }
        val cancellation = cas3CancellationEntityFactory.produceAndPersist {
          withBooking(booking)
          withReason(cancellationReason)
        }
        booking.cancellations = mutableListOf(cancellation)

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/cancellations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            NewCancellation(
              date = LocalDate.parse("2022-08-18"),
              reason = cancellationReason.id,
              notes = "Corrected date",
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath(".bookingId").isEqualTo(booking.id.toString())
          .jsonPath(".date").isEqualTo("2022-08-18")
          .jsonPath(".notes").isEqualTo("Corrected date")
          .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
          .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

        assertPublishedSnsEvent(
          booking,
          DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
          "A cancelled booking for a Transitional Accommodation premises has been updated",
          "http://api/events/cas3/booking-cancelled-updated",
        )
      }
    }

    @Test
    fun `Create Cancellation on CAS3 Booking when no cancellation exists against the booking already returns OK with correct body and sends cancelled event`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
        }
        val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
          withServiceScope("*")
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/cancellations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            NewCancellation(
              date = LocalDate.parse("2022-08-18"),
              reason = cancellationReason.id,
              notes = "Corrected date",
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath(".bookingId").isEqualTo(booking.id.toString())
          .jsonPath(".date").isEqualTo("2022-08-18")
          .jsonPath(".notes").isEqualTo("Corrected date")
          .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
          .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
          .jsonPath(".reason.isActive").isEqualTo(true)
          .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

        assertPublishedSnsEvent(
          booking,
          DomainEventType.CAS3_BOOKING_CANCELLED,
          "A booking for a Transitional Accommodation premises has been cancelled",
          "http://api/events/cas3/booking-cancelled",
        )
      }
    }

    @Test
    fun `Create Cancellation on CAS3 Booking when a cancellation already exists returns OK with correct body and move assessment to ready-to-place state`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val (application, assessment) = givenCas3ApplicationAndAssessment(user, offenderDetails)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withApplication(application)
          }
          val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
            withServiceScope("*")
          }
          val cancellation = cas3CancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReason)
          }
          booking.cancellations = mutableListOf(cancellation)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/cancellations")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCancellation(
                date = LocalDate.parse("2022-08-18"),
                reason = cancellationReason.id,
                notes = "Corrected date",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath(".bookingId").isEqualTo(booking.id.toString())
            .jsonPath(".date").isEqualTo("2022-08-18")
            .jsonPath(".notes").isEqualTo("Corrected date")
            .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
            .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
            "A cancelled booking for a Transitional Accommodation premises has been updated",
            "http://api/events/cas3/booking-cancelled-updated",
          )

          assertCAS3AssessmentIsReadyToPlace(assessmentId = assessment.id)
        }
      }
    }

    @Test
    fun `Create Cancellation on CAS3 Booking returns OK and make move assessment to ready-to-place state when accept assessment fail with forbidden exception`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(user)
          val (application, assessment) = givenCas3ApplicationAndAssessment(
            user,
            offenderDetails,
            assessmentRelocatedAt = OffsetDateTime.now(),
          )
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withApplication(application)
          }
          val cancellationReason = cancellationReasonEntityFactory.produceAndPersist {
            withServiceScope("*")
          }
          val cancellation = cas3CancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReason)
          }
          booking.cancellations = mutableListOf(cancellation)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/cancellations")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewCancellation(
                date = LocalDate.parse("2022-08-18"),
                reason = cancellationReason.id,
                notes = "Corrected date",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath(".bookingId").isEqualTo(booking.id.toString())
            .jsonPath(".date").isEqualTo("2022-08-18")
            .jsonPath(".notes").isEqualTo("Corrected date")
            .jsonPath(".reason.id").isEqualTo(cancellationReason.id.toString())
            .jsonPath(".reason.name").isEqualTo(cancellationReason.name)
            .jsonPath(".reason.isActive").isEqualTo(true)
            .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_BOOKING_CANCELLED_UPDATED,
            "A cancelled booking for a Transitional Accommodation premises has been updated",
            "http://api/events/cas3/booking-cancelled-updated",
          )

          assertCAS3AssessmentIsClosed(assessment)
        }
      }
    }
  }

  @Nested
  inner class CreateDeparture {

    @Test
    fun `Create Departure updates the departure date for a booking`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
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
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/departures")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              Cas3NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                notes = "Hello",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.arrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.departureDate").isEqualTo("2022-09-01")
            .jsonPath("$.originalArrivalDate").isEqualTo("2022-08-10")
            .jsonPath("$.originalDepartureDate").isEqualTo("2022-08-30")
            .jsonPath("$.createdAt").isEqualTo("2022-07-01T12:34:56.789Z")

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_PERSON_DEPARTED,
            "Someone has left a Transitional Accommodation premises",
            "http://api/events/cas3/person-departed",
          )
        }
      }
    }

    @Test
    fun `Create Departure on Booking when a departure already exists returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
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
          val departure = cas3DepartureEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(reason)
            withMoveOnCategory(moveOnCategory)
          }
          booking.departures = mutableListOf(departure)

          webTestClient.post()
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/departures")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              Cas3NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                notes = "Corrected date",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.dateTime").isEqualTo("2022-09-01T12:34:56.789Z")
            .jsonPath("$.reason.id").isEqualTo(reason.id.toString())
            .jsonPath("$.moveOnCategory.id").isEqualTo(moveOnCategory.id.toString())
            .jsonPath("$.notes").isEqualTo("Corrected date")
            .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

          assertPublishedSnsEvent(
            booking,
            DomainEventType.CAS3_PERSON_DEPARTURE_UPDATED,
            "Person has updated departure date of Transitional Accommodation premises",
            "http://api/events/cas3/person-departure-updated",
          )
        }
      }
    }

    @Test
    fun `Create Departure for a booking on a premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = givenACas3Premises()
        givenAnOffender { offenderDetails, inmateDetails ->
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(
              cas3BedspaceEntityFactory.produceAndPersist {
                withPremises(premises)
              },
            )
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
            .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/departures")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              Cas3NewDeparture(
                dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                notes = "Hello",
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Create Departure for a booking on a premises that does not exist returns 404 Not Found`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
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

          val notFoundPremisesId = UUID.randomUUID()

          webTestClient.post()
            .uri("/cas3/v2/premises/$notFoundPremisesId/bookings/${booking.id}/departures")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              Cas3NewDeparture(
                dateTime = Instant.now().plus(10, ChronoUnit.DAYS),
                reasonId = reason.id,
                moveOnCategoryId = moveOnCategory.id,
                notes = "Some notes",
              ),
            )
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Premises with an ID of $notFoundPremisesId could be found")
        }
      }
    }
  }

  @Nested
  inner class CreateConfirmation {

    @Test
    fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            NewConfirmation(
              notes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
          .jsonPath("$.dateTime").value(OffsetDateTime::class.java, withinSeconds(5L))
          .jsonPath("$.notes").value(nullValue())
          .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))
      }
    }

    @Test
    fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body and close associated referral`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withProbationRegion(userEntity.probationRegion)
            withCreatedByUser(userEntity)
            withCrn(offenderDetails.otherIds.crn)
          }

          val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
          }

          val booking = cas3BookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBedspace(bedspace)
            withApplication(application)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          assertCAS3AssessmentIsNotClosed(assessment)

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/confirmations")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewConfirmation(
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
            .jsonPath("$.dateTime").value(OffsetDateTime::class.java, withinSeconds(5L))
            .jsonPath("$.notes").value(nullValue())
            .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

          assertCAS3AssessmentIsClosed(assessment)
        }
      }
    }

    @Test
    fun `Create Confirmation on Temporary Accommodation Booking for a premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { userEntity, jwt ->
        val premises = givenACas3Premises()
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(
            cas3BedspaceEntityFactory.produceAndPersist {
              withPremises(premises)
            },
          )
          withServiceName(ServiceName.temporaryAccommodation)
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBedspace(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
        }

        val notFoundPremisesId = UUID.randomUUID()

        webTestClient.post()
          .uri("/cas3/v2/premises/$notFoundPremisesId/bookings/${booking.id}/confirmations")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            NewConfirmation(
              notes = null,
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $notFoundPremisesId could be found")
      }
    }
  }

  @Nested
  inner class CreateExtension {

    @Test
    fun `Create Extension returns 409 Conflict when another booking for the same bed overlaps with the new departure date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val conflictingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/extensions")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-16"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .withConflictMessage("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @Test
    fun `Create Extension returns 409 Conflict when another booking for the same bed overlaps with the updated booking's turnaround time`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val conflictingBooking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-07-15"))
            withDepartureDate(LocalDate.parse("2022-08-15"))
          }
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-12"))
            withDepartureDate(LocalDate.parse("2022-07-12"))
          }
          val turnarounds = cas3v2TurnaroundFactory.produceAndPersistMultiple(1) {
            withWorkingDayCount(2)
            withCreatedAt(booking.createdAt)
            withBooking(booking)
          }
          booking.turnarounds += turnarounds

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/extensions")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-13"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .withConflictMessage("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingBooking.id}")
        }
      }
    }

    @Test
    fun `Create Extension returns 409 Conflict when a void bedspace for the same bed overlaps with the new departure date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-14"))
            withDepartureDate(LocalDate.parse("2022-07-14"))
          }

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/extensions")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-16"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .withConflictMessage("A Void Bedspace already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create Extension returns 409 Conflict when a void bedspace for the same bed overlaps with the updated booking's turnaround time`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
          val conflictingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBedspace(bedspace)
            withStartDate(LocalDate.parse("2022-07-15"))
            withEndDate(LocalDate.parse("2022-08-15"))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }
          val booking = cas3BookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(offenderDetails.otherIds.crn)
            withPremises(premises)
            withBedspace(bedspace)
            withArrivalDate(LocalDate.parse("2022-06-12"))
            withDepartureDate(LocalDate.parse("2022-07-12"))
          }
          val turnarounds = cas3v2TurnaroundFactory.produceAndPersistMultiple(1) {
            withWorkingDayCount(2)
            withCreatedAt(booking.createdAt)
            withBooking(booking)
          }
          booking.turnarounds += turnarounds

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          webTestClient.post()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/extensions")
            .headers(buildTemporaryAccommodationHeaders(jwt))
            .bodyValue(
              NewExtension(
                newDepartureDate = LocalDate.parse("2022-07-13"),
                notes = null,
              ),
            )
            .exchange()
            .expectStatus()
            .is4xxClientError
            .withConflictMessage("A Void Bedspace already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${conflictingLostBed.id}")
        }
      }
    }

    @Test
    fun `Create Extension returns 403 Forbidden for a premises that's not in the user's region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = givenACas3Premises()
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withDepartureDate(LocalDate.parse("2022-08-20"))
          withPremises(premises)
          withBedspace(
            cas3BedspaceEntityFactory.produceAndPersist {
              withPremises(premises)
            },
          )
        }

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}/extensions")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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

    @Test
    fun `Create Extension returns OK with expected body, updates departureDate on Booking entity when user has one of roles CAS3_ASSESSOR`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val (premises, bedspace) = givenCas3PremisesAndBedspace(userEntity)
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withArrivalDate(LocalDate.parse("2022-08-18"))
          withDepartureDate(LocalDate.parse("2022-08-20"))
          withPremises(premises)
          withBedspace(bedspace)
        }
        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val newDate = "2022-08-22"
        webTestClient.post()
          .uri("/cas3/v2/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(
            NewExtension(
              newDepartureDate = LocalDate.parse(newDate),
              notes = "notes",
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath(".bookingId").isEqualTo(booking.id.toString())
          .jsonPath(".previousDepartureDate").isEqualTo(booking.departureDate.toString())
          .jsonPath(".newDepartureDate").isEqualTo(newDate)
          .jsonPath(".notes").isEqualTo("notes")
          .jsonPath("$.createdAt").value(OffsetDateTime::class.java, withinSeconds(5L))

        val actualBooking = bookingRepository.findByIdOrNull(booking.id)
        assertThat(actualBooking?.departureDate).isEqualTo(LocalDate.parse(newDate))
        assertThat(actualBooking?.originalDepartureDate).isEqualTo(booking.departureDate)
      }
    }
  }

  private fun assertCAS3AssessmentIsNotClosed(assessment: TemporaryAccommodationAssessmentEntity) {
    val temporaryAccommodationAssessmentEntity =
      temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)

    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNull()
  }

  private fun assertPublishedSnsEvent(
    booking: Cas3BookingEntity,
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

  private fun assertCAS3AssessmentIsReadyToPlace(assessmentId: UUID) {
    val temporaryAccommodationAssessmentEntity = temporaryAccommodationAssessmentRepository.findByIdOrNull(assessmentId)
    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNull()
    assertThat(temporaryAccommodationAssessmentEntity.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(temporaryAccommodationAssessmentEntity.submittedAt).isNotNull()
  }

  private fun assertCAS3AssessmentIsClosed(assessment: TemporaryAccommodationAssessmentEntity) {
    val temporaryAccommodationAssessmentEntity =
      temporaryAccommodationAssessmentRepository.findByIdOrNull(assessment.id)

    assertThat(temporaryAccommodationAssessmentEntity!!.completedAt).isNotNull()
  }
}
