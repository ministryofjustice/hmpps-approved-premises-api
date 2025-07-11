package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3.v2

import org.hamcrest.core.IsNull.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenCas3PremisesAndBedspaceAndApplicationAndAssessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .uri("/cas3/v2/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a booking for a premises returns 403 Forbidden when not a CAS3_ASSESSOR`() {
      givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { user, jwt ->
        givenAnOffender { offenderDetails, _ ->
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
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .uri("/cas3/v2/premises/${UUID.randomUUID()}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          withCrn("SOME-CRN")
          withServiceName(ServiceName.temporaryAccommodation)
        }

        webTestClient.get()
          .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
            withNomsNumber(null)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bookings/${booking.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
  fun `Get all Bookings returns OK with correct body when user has CAS3_ASSESSOR role`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )

        val bookings = cas3BookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(cas3Premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
          withBedspace(
            cas3BedspaceEntityFactory.produceAndPersist {
              withPremises(cas3Premises)
            },
          )
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
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
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
      val cas3Premises = givenACas3Premises(
        probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        },
      )
      val booking = cas3BookingEntityFactory.produceAndPersist {
        withPremises(cas3Premises)
        withCrn("SOME-CRN")
        withServiceName(ServiceName.temporaryAccommodation)
        withBedspace(
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(cas3Premises)
          },
        )
      }

      val expectedJson = objectMapper.writeValueAsString(
        listOf(
          bookingTransformer.transformJpaToApi(
            booking,
            PersonInfoResult.NotFound("SOME-CRN"),
          ),
        ),
      )

      webTestClient.get()
        .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
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
        val cas3Premises = givenACas3Premises(
          probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user.probationRegion)
          },
        )
        val booking = cas3BookingEntityFactory.produceAndPersist {
          withPremises(cas3Premises)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.temporaryAccommodation)
          withBedspace(
            cas3BedspaceEntityFactory.produceAndPersist {
              withPremises(cas3Premises)
            },
          )
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
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          premises = cas3Premises,
          bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(cas3Premises)
            withEndDate(arrivalDate.minusDays(1))
          },
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          premises = cas3Premises,
          bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(cas3Premises)
            withEndDate(arrivalDate)
          },
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          premises = cas3Premises,
          bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(cas3Premises)
            withEndDate(departureDate.minusDays(1))
          },
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("BedSpace is archived from ${bedspace.endDate} which overlaps with the desired dates: ${bedspace.id}")
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          premises = cas3Premises,
          bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(cas3Premises)
            withStartDate(arrivalDate.minusDays(1))
            withEndDate(departureDate)
          },
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          premises = cas3Premises,
          bedspace = cas3BedspaceEntityFactory.produceAndPersist {
            withReference("test-bed")
            withPremises(cas3Premises)
            withStartDate(arrivalDate.minusDays(1))
            withEndDate(departureDate.plusDays(1))
          },
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${cas3Premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          startDate = arrivalDate.minusDays(1),
        )

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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails)

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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns 409 Conflict when another booking for the same bedspace has a turnaround that overlaps with the desired dates`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails)

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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Booking already exists for dates from 2022-07-15 to 2022-08-22 which overlaps with the desired dates: ${existingBooking.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when only cancelled bookings for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-07-15")
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))

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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails)
        val existingLostBed = cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBedspace(bedspace)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .expectBody()
          .jsonPath("title").isEqualTo("Conflict")
          .jsonPath("status").isEqualTo(409)
          .jsonPath("detail")
          .isEqualTo("A Lost Bed already exists for dates from 2022-07-15 to 2022-08-15 which overlaps with the desired dates: ${existingLostBed.id}")
      }
    }
  }

  @Test
  fun `Create Booking returns OK with correct body when only cancelled void bedspaces for the same bed overlap`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val arrivalDate = LocalDate.parse("2022-07-15")
        val (premises, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(user, offenderDetails, startDate = arrivalDate.minusDays(1))
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
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
          .isOk
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
        val (_, bedspace, assessment) = givenCas3PremisesAndBedspaceAndApplicationAndAssessment(
          user,
          offenderDetails,
          startDate = arrivalDate.minusDays(1),
          premises,
        )
        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
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
}
