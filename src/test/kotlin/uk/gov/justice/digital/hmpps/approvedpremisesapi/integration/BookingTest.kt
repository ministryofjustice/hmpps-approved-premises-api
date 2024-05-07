package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas1Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewNonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.APDeliusContext_mockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
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
import java.util.UUID

class BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Nested
  inner class GetBooking {

    @Test
    fun `Get a booking without JWT returns 401`() {
      webTestClient.get()
        .uri("/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a non existant booking returns 404`() {
      webTestClient.get()
        .uri("/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `Get a booking returns OK with the correct body when user has one of roles MANAGER, MATCHER`(
      role: UserRole,
    ) {
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
            .uri("/bookings/${booking.id}")
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
                  keyWorker,
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
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/27a596af-ce14-4616-b734-420f5c5fc242")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get a booking belonging to another premises returns not found`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
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

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
    fun `Get a booking for an Approved Premises returns OK with the correct body when user has one of roles MANAGER, MATCHER`(
      role: UserRole,
    ) {
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
                bookingTransformer.transformJpaToApi(
                  booking,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  keyWorker,
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Get a booking returns OK with the correct body when person details for a booking could not be found`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { _, jwt ->
        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn("SOME-CRN")
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
              bookingTransformer.transformJpaToApi(
                booking,
                PersonInfoResult.NotFound("SOME-CRN"),
                keyWorker,
              ),
            ),
          )
      }
    }

    @Test
    fun `Get a booking for an Approved Premises returns OK with the correct body when the NOMS number is null`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
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
            withNomsNumber(null)
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
                bookingTransformer.transformJpaToApi(
                  booking,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, null),
                  keyWorker,
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Get a booking for an Temporary Accommodation Premises returns OK with the correct body`() {
      `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
                bookingTransformer.transformJpaToApi(
                  booking,
                  PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetails),
                  null,
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Get a booking for a Temporary Accommodation Premises not in the user's region returns 403 Forbidden`() {
      `Given a User` { userEntity, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withId(UUID.randomUUID())
                withYieldedApArea {
                  apAreaEntityFactory.produceAndPersist()
                }
              }
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
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Get all Bookings on Premises without any Bookings returns empty list when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
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
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
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
              keyWorker,
            )
          },
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
  fun `Get all Bookings returns OK with correct body when person details for a booking could not be found`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withStaffKeyWorkerCode(keyWorker.code)
        withCrn("SOME-CRN")
        withServiceName(ServiceName.approvedPremises)
      }

      val expectedJson = objectMapper.writeValueAsString(
        listOf(
          bookingTransformer.transformJpaToApi(
            booking,
            PersonInfoResult.NotFound("SOME-CRN"),
            keyWorker,
          ),
        ),
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

  @Test
  fun `Get all Bookings returns OK with correct body when inmate details for a booking could not be found`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { userEntity, jwt ->
      `Given an Offender`(mockServerErrorForPrisonApi = true) { offenderDetails, inmateDetails ->

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, premises.qCode)

        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withStaffKeyWorkerCode(keyWorker.code)
          withCrn(offenderDetails.otherIds.crn)
          withServiceName(ServiceName.approvedPremises)
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
              keyWorker,
            ),
          ),
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
  fun `Get all Bookings on a Temporary Accommodation premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { _, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withId(UUID.randomUUID())
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }

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
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .bodyValue(
        NewBooking(
          crn = "a crn",
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.approvedPremises,
          bedId = UUID.randomUUID(),
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Nested
  inner class CreateCas1AdhocBooking {

    @Test
    fun `Create Approved Premises Booking returns OK with correct body emits domain event and sends emails`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { _, jwt ->
        `Given a User` { applicant, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val premises = approvedPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
              }
            }

            val linkedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicant)
              withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
              withSubmittedAt(OffsetDateTime.now())
            }

            val room = roomEntityFactory.produceAndPersist {
              withPremises(premises)
            }

            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            webTestClient.post()
              .uri("/premises/${premises.id}/bookings")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                NewBooking(
                  crn = offenderDetails.otherIds.crn,
                  arrivalDate = LocalDate.parse("2022-08-12"),
                  departureDate = LocalDate.parse("2022-08-30"),
                  serviceName = ServiceName.approvedPremises,
                  bedId = bed.id,
                  eventNumber = "eventNumber",
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
              .jsonPath("$.status").isEqualTo("awaiting-arrival")
              .jsonPath("$.arrival").isEqualTo(null)
              .jsonPath("$.departure").isEqualTo(null)
              .jsonPath("$.nonArrival").isEqualTo(null)
              .jsonPath("$.cancellation").isEqualTo(null)
              .jsonPath("$.serviceName").isEqualTo(ServiceName.approvedPremises.value)
              .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
              .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
              .jsonPath("$.bed.name").isEqualTo(bed.name)

            val emittedMessage = snsDomainEventListener.blockForMessage(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)

            assertThat(emittedMessage.description).isEqualTo("An Approved Premises booking has been made")
            assertThat(emittedMessage.detailUrl).matches("http://api/events/booking-made/[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}")
            assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(linkedApplication.id)
            assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
              SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
              SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
            )

            emailAsserter.assertEmailsRequestedCount(2)
            emailAsserter.assertEmailRequested(
              applicant.email!!,
              notifyConfig.templates.bookingMade,
            )
            emailAsserter.assertEmailRequested(
              premises.emailAddress!!,
              notifyConfig.templates.bookingMadePremises,
            )
          }
        }
      }
    }

    @Test
    fun `Create Approved Premises Booking returns OK with correct body when NOMS number is null`() {
      `Given a User`(roles = listOf(UserRole.CAS1_MATCHER)) { userEntity, jwt ->
        `Given an Offender`(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
            }
          }

          val linkedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(userEntity)
            withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
            withSubmittedAt(OffsetDateTime.now())
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          webTestClient.post()
            .uri("/premises/${premises.id}/bookings")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewBooking(
                crn = offenderDetails.otherIds.crn,
                arrivalDate = LocalDate.parse("2022-08-12"),
                departureDate = LocalDate.parse("2022-08-30"),
                serviceName = ServiceName.approvedPremises,
                bedId = bed.id,
                eventNumber = "eventNumber",
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
            .jsonPath("$.status").isEqualTo("awaiting-arrival")
            .jsonPath("$.arrival").isEqualTo(null)
            .jsonPath("$.departure").isEqualTo(null)
            .jsonPath("$.nonArrival").isEqualTo(null)
            .jsonPath("$.cancellation").isEqualTo(null)
            .jsonPath("$.serviceName").isEqualTo(ServiceName.approvedPremises.value)
            .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
            .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
            .jsonPath("$.bed.name").isEqualTo(bed.name)
        }
      }
    }
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val arrivalDate = LocalDate.parse("2022-08-12")
        val departureDate = LocalDate.parse("2022-08-30")

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(applicationSchema)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      `Given an Offender`(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val existingTurnarounds = turnaroundFactory.produceAndPersistMultiple(1) {
          withWorkingDayCount(5)
          withCreatedAt(existingBooking.createdAt)
          withBooking(existingBooking)
        }

        existingBooking.turnarounds += existingTurnarounds

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        existingBooking.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
          withYieldedBooking { existingBooking }
          withDate(LocalDate.parse("2022-07-01"))
          withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
        }.toMutableList()

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Booking returns 409 Conflict when a lost bed for the same bed overlaps`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val existingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Booking returns 409 Conflict when a lost bed for the same bed overlaps with the turnaround time`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
          }
          withTurnaroundWorkingDayCount(2)
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

        val existingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Booking returns OK with correct body when only cancelled lost beds for the same bed overlap`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val existingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason {
            lostBedReasonEntityFactory.produceAndPersist()
          }
        }

        existingLostBed.cancellation = lostBedCancellationEntityFactory.produceAndPersist {
          withLostBed(existingLostBed)
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withId(UUID.randomUUID())
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Arrival returns 409 Conflict when a lost bed for the same bed overlaps with the arrival and expected departure dates`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val conflictingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Arrival on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1Arrival(
            type = "CAS1",
            arrivalDateTime = Instant.parse("2022-08-12T15:30:00Z"),
            expectedDepartureDate = LocalDate.parse("2022-08-14"),
            notes = "Hello",
            keyWorkerStaffCode = keyWorker.code,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
        .jsonPath("$.arrivalTime").isEqualTo("15:30:00")
        .jsonPath("$.expectedDepartureDate").isEqualTo("2022-08-14")
        .jsonPath("$.notes").isEqualTo("Hello")
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Arrival on Approved Premises Booking returns 200 with correct body when over-booking`(role: UserRole) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val conflictingBooking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.of(2023, 6, 1))
        withDepartureDate(LocalDate.of(2023, 6, 10))
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.of(2023, 5, 20))
        withDepartureDate(LocalDate.of(2023, 5, 30))
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1Arrival(
            type = "CAS1",
            arrivalDateTime = Instant.parse("2023-05-20T15:00:00Z"),
            expectedDepartureDate = LocalDate.parse("2023-06-05"),
            notes = "Hello",
            keyWorkerStaffCode = keyWorker.code,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.arrivalDate").isEqualTo("2023-05-20")
        .jsonPath("$.expectedDepartureDate").isEqualTo("2023-06-05")
        .jsonPath("$.notes").isEqualTo("Hello")
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @Test
  fun `Create Arrival on Approved Premises Booking returns 200 with correct body when a bed is not present`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist {
            withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
          }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withArrivalDate(LocalDate.of(2023, 5, 20))
        withDepartureDate(LocalDate.of(2023, 5, 30))
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewCas1Arrival(
            type = "CAS1",
            arrivalDateTime = Instant.parse("2023-05-20T15:00:00Z"),
            expectedDepartureDate = LocalDate.parse("2023-06-05"),
            notes = "Hello",
            keyWorkerStaffCode = keyWorker.code,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.arrivalDate").isEqualTo("2023-05-20")
        .jsonPath("$.expectedDepartureDate").isEqualTo("2023-06-05")
        .jsonPath("$.notes").isEqualTo("Hello")
        .jsonPath("$.createdAt").value(withinSeconds(5L), OffsetDateTime::class.java)
    }
  }

  @Test
  fun `Create Arrival updates arrival and departure date for an Approved Premises booking`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withQCode("QCODE")
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withPremises(premises)
          withBed(bed)
          withArrivalDate(LocalDate.parse("2022-08-10"))
          withDepartureDate(LocalDate.parse("2022-08-30"))
          withCreatedAt(OffsetDateTime.parse("2022-07-01T12:34:56.789Z"))
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewCas1Arrival(
              type = "CAS1",
              arrivalDateTime = Instant.parse("2022-08-12T15:00:00Z"),
              expectedDepartureDate = LocalDate.parse("2022-08-14"),
              notes = "Hello",
              keyWorkerStaffCode = keyWorker.code,
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
  fun `Create Arrival updates arrival and departure date for a Temporary Accommodation booking`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
        booking.let {
          it.arrivals = arrivalEntityFactory.produceAndPersistMultiple(2) { withBooking(it) }.toMutableList()
        }

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/arrivals")
          .header("Authorization", "Bearer $jwt")
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
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withId(UUID.randomUUID())
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Departure on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
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
            dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
            reasonId = reason.id,
            moveOnCategoryId = moveOnCategory.id,
            destinationProviderId = destinationProvider.id,
            notes = "Hello",
          ),
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
  fun `Create Departure updates departure date for an Approved Premises booking`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val keyWorker = ContextStaffMemberFactory().produce()
        APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withQCode("QCODE")
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
            }
          }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withPremises(premises)
          withBed(bed)
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
              dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
              reasonId = reason.id,
              moveOnCategoryId = moveOnCategory.id,
              destinationProviderId = destinationProvider.id,
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Departure on Approved Premises Booking when a departure already exists returns 400 Bad Request`(role: UserRole) {
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

      val departure = departureEntityFactory.produceAndPersist {
        withBooking(booking)
        withReason(reason)
        withMoveOnCategory(moveOnCategory)
        withDestinationProvider(destinationProvider)
      }
      booking.departures = mutableListOf(departure)

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewDeparture(
            dateTime = Instant.parse("2022-09-01T12:34:56.789Z"),
            reasonId = reason.id,
            moveOnCategoryId = moveOnCategory.id,
            destinationProviderId = destinationProvider.id,
            notes = "Corrected date",
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath(".detail").isEqualTo("This Booking already has a Departure set")
    }
  }

  @Test
  fun `Create Departure updates the departure date for a Temporary Accommodation booking`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val departure = departureEntityFactory.produceAndPersist {
          withBooking(booking)
          withReason(reason)
          withMoveOnCategory(moveOnCategory)
        }
        booking.departures = mutableListOf(departure)

        webTestClient.post()
          .uri("/premises/${booking.premises.id}/bookings/${booking.id}/departures")
          .header("Authorization", "Bearer $jwt")
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

  @Test
  fun `Create Departure for a Temporary Accommodation booking on a premises that's not in the user's region returns 403 Forbidden`() {
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

  @Nested
  inner class CreateCancellationCas1 {

    @Test
    fun `Create Cancellation without JWT returns 401`() {
      webTestClient.post()
        .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/cancellations")
        .bodyValue(
          NewCancellation(
            date = LocalDate.parse("2022-08-17"),
            reason = UUID.fromString("070149f6-c194-4558-a027-f67a10da7865"),
            notes = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Create Cancellation with invalid role returns 401`(role: UserRole) {
      `Given a User`(roles = listOf(role)) { _, jwt ->
        `Given a User`(roles = listOf(role)) { applicant, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val apArea = apAreaEntityFactory.produceAndPersist {
              withEmailAddress("apAreaEmail@test.com")
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicant)
              withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
              withApArea(apArea)
              withSubmittedAt(OffsetDateTime.now())
            }

            val booking = bookingEntityFactory.produceAndPersist {
              withYieldedPremises {
                approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion {
                    probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                  }
                }
              }
              withCrn(offenderDetails.otherIds.crn)
              withApplication(application)
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
                  notes = null,
                ),
              )
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Create Cancellation on CAS1 Booking returns OK with correct body and sends emails when user has one of roles WORKFLOW_MANAGER`() {
      `Given a User`(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER)) { _, jwt ->
        `Given a User` { applicant, _ ->
          `Given an Offender` { offenderDetails, _ ->
            val apArea = apAreaEntityFactory.produceAndPersist {
              withEmailAddress("apAreaEmail@test.com")
            }

            val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(applicant)
              withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
              withApArea(apArea)
              withSubmittedAt(OffsetDateTime.now())
            }

            val booking = bookingEntityFactory.produceAndPersist {
              withYieldedPremises {
                approvedPremisesEntityFactory.produceAndPersist {
                  withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
                  withYieldedProbationRegion {
                    probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
                  }
                }
              }
              withCrn(offenderDetails.otherIds.crn)
              withApplication(application)
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
                  notes = null,
                ),
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

            val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(booking.application!!.id)!!
            assertThat(updatedApplication.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)

            emailAsserter.assertEmailsRequestedCount(3)
            emailAsserter.assertEmailRequested(applicant.email!!, notifyConfig.templates.bookingWithdrawnV2)
            emailAsserter.assertEmailRequested(booking.premises.emailAddress!!, notifyConfig.templates.bookingWithdrawnV2)
            emailAsserter.assertEmailRequested(apArea.emailAddress!!, notifyConfig.templates.bookingWithdrawnV2)
          }
        }
      }
    }
  }

  @Test
  fun `Create Cancellation on CAS3 Booking on a premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withId(UUID.randomUUID())
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
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
  fun `Create Cancellation on CAS3 Booking when a cancellation already exists returns OK with correct body and send cancelled-updated event`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withProbationRegion(userEntity.probationRegion)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
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
  fun `Create Temporary Accommodation Extension returns 409 Conflict when another booking for the same bed overlaps with the new departure date`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Extension returns 409 Conflict when another booking for the same bed overlaps with the updated booking's turnaround time`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
          withArrivalDate(LocalDate.parse("2022-06-12"))
          withDepartureDate(LocalDate.parse("2022-07-12"))
        }

        val turnarounds = turnaroundFactory.produceAndPersistMultiple(1) {
          withWorkingDayCount(2)
          withCreatedAt(booking.createdAt)
          withBooking(booking)
        }

        booking.turnarounds += turnarounds

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Extension returns 409 Conflict when a lost bed for the same bed overlaps with the new departure date`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val conflictingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-06-14"))
          withDepartureDate(LocalDate.parse("2022-07-14"))
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
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
  fun `Create Temporary Accommodation Extension returns 409 Conflict when a lost bed for the same bed overlaps with the updated booking's turnaround time`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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

        val conflictingLostBed = lostBedsEntityFactory.produceAndPersist {
          withBed(bed)
          withPremises(premises)
          withStartDate(LocalDate.parse("2022-07-15"))
          withEndDate(LocalDate.parse("2022-08-15"))
          withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
        }

        val booking = bookingEntityFactory.produceAndPersist {
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.otherIds.crn)
          withYieldedPremises { premises }
          withYieldedBed { bed }
          withArrivalDate(LocalDate.parse("2022-06-12"))
          withDepartureDate(LocalDate.parse("2022-07-12"))
        }

        val turnarounds = turnaroundFactory.produceAndPersistMultiple(1) {
          withWorkingDayCount(2)
          withCreatedAt(booking.createdAt)
          withBooking(booking)
        }

        booking.turnarounds += turnarounds

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        webTestClient.post()
          .uri("/premises/${premises.id}/bookings/${booking.id}/extensions")
          .header("Authorization", "Bearer $jwt")
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Extension on Approved Premises Booking returns OK with expected body, updates departureDate on Booking entity when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
        withBed(bed)
      }

      creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-08-22")
    }
  }

  @Test
  fun `Create Extension on Approved Premises Booking returns OK when a booking has no bed`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { userEntity, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
      }

      creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-08-22")
    }
  }

  @Test
  fun `Create Approved Premises Extension returns OK when another booking for the same bed overlaps with the new departure date`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
        withBed(bed)
      }

      val conflictingBooking = bookingEntityFactory.produceAndPersist {
        withServiceName(ServiceName.temporaryAccommodation)
        withCrn("CRN123")
        withYieldedPremises { premises }
        withYieldedBed { bed }
        withArrivalDate(LocalDate.parse("2022-07-15"))
        withDepartureDate(LocalDate.parse("2022-08-15"))
      }

      creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-09-20")
    }
  }

  @Test
  fun `Create Approved Premises Extension returns OK when another booking for the same bed overlaps with the updated booking's turnaround time`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
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
        withArrivalDate(LocalDate.parse("2022-08-21"))
        withDepartureDate(LocalDate.parse("2022-08-29"))
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
        withBed(bed)
      }

      creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-08-23")
    }
  }

  @Test
  fun `Create Approved Premises Extension returns OK when a lost bed for the same bed overlaps with the new departure date`() {
    `Given a User`(roles = listOf(UserRole.CAS1_MANAGER)) { _, jwt ->
      val keyWorker = ContextStaffMemberFactory().produce()
      APDeliusContext_mockSuccessfulStaffMembersCall(keyWorker, "QCODE")

      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withQCode("QCODE")
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

      val conflictingLostBed = lostBedsEntityFactory.produceAndPersist {
        withBed(bed)
        withPremises(premises)
        withStartDate(LocalDate.parse("2022-07-15"))
        withEndDate(LocalDate.parse("2022-08-22"))
        withYieldedReason { lostBedReasonEntityFactory.produceAndPersist() }
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withStaffKeyWorkerCode(keyWorker.code)
        withPremises(premises)
        withBed(bed)
      }

      creatingNewExtensionReturnsCorrectly(booking, jwt, "2022-08-29")
    }
  }

  @Test
  fun `Create Extension on Temporary Accommodation Booking for a premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withId(UUID.randomUUID())
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
            }
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

  @Test
  fun `Create Date Change without JWT returns 401`() {
    webTestClient.post()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings/1617e729-13f3-4158-bd88-c59affdb8a45/date-changes")
      .bodyValue(
        NewDateChange(
          newArrivalDate = null,
          newDepartureDate = null,
        ),
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create AP Date Change without MANAGER or MATCHER role returns 403`() {
    `Given a User`(roles = emptyList()) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withPremises(premises)
        withBed(bed)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/date-changes")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewDateChange(
            newArrivalDate = null,
            newDepartureDate = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create AP Date Change with MANAGER or MATCHER role returns 200, persists date change`(role: UserRole) {
    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

    `Given a User`(roles = listOf(role)) { userEntity, jwt ->
      val premises = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withYieldedProbationRegion {
          probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withArrivalDate(LocalDate.parse("2022-08-18"))
        withDepartureDate(LocalDate.parse("2022-08-20"))
        withPremises(premises)
        withBed(bed)
      }

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/date-changes")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewDateChange(
            newArrivalDate = LocalDate.parse("2023-07-13"),
            newDepartureDate = LocalDate.parse("2023-07-15"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedBooking = bookingRepository.findByIdOrNull(booking.id)!!

      assertThat(persistedBooking.arrivalDate).isEqualTo(LocalDate.parse("2023-07-13"))
      assertThat(persistedBooking.departureDate).isEqualTo(LocalDate.parse("2023-07-15"))
      assertThat(persistedBooking.dateChanges).singleElement()
      val persistedDateChange = persistedBooking.dateChanges.first()
      assertThat(persistedDateChange.previousArrivalDate).isEqualTo(LocalDate.parse("2022-08-18"))
      assertThat(persistedDateChange.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-20"))
      assertThat(persistedDateChange.newArrivalDate).isEqualTo(LocalDate.parse("2023-07-13"))
      assertThat(persistedDateChange.newDepartureDate).isEqualTo(LocalDate.parse("2023-07-15"))
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Confirmation on Approved Premises Booking returns OK with correct body when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
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
        withServiceName(ServiceName.approvedPremises)
      }

      webTestClient.post()
        .uri("/premises/${booking.premises.id}/bookings/${booking.id}/confirmations")
        .header("Authorization", "Bearer $jwt")
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
  fun `Create Confirmation on Temporary Accommodation Booking returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
          withCreatedByUser(userEntity)
          withApplicationSchema(applicationSchema)
          withCrn(offenderDetails.otherIds.crn)
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
          withAddedAt(OffsetDateTime.now())
        }

        val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAssessmentSchema(assessmentSchema)
        }
        assessment.schemaUpToDate = true

        val booking = bookingEntityFactory.produceAndPersist {
          withYieldedPremises {
            temporaryAccommodationPremisesEntityFactory.produceAndPersist {
              withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
              withYieldedProbationRegion {
                probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
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

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = ["CAS1_MANAGER", "CAS1_MATCHER"])
  fun `Create Non Arrival on Approved Premises Booking returns 200 with correct body when user has one of roles MANAGER, MATCHER`(
    role: UserRole,
  ) {
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
            notes = "Notes",
          ),
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

  @Test
  fun `Create Confirmation on Temporary Accommodation Booking for a premises that's not in the user's region returns 403 Forbidden`() {
    `Given a User` { userEntity, jwt ->
      val booking = bookingEntityFactory.produceAndPersist {
        withYieldedPremises {
          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion {
              probationRegionEntityFactory.produceAndPersist {
                withId(UUID.randomUUID())
                withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
              }
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
        .isForbidden
    }
  }

  @Test
  fun `Successfully send updated departure date events when create date-change is invoked for existing booking with departure detail`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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

  private fun createTemporaryAccommodationPremises(userEntity: UserEntity) =
    temporaryAccommodationPremisesEntityFactory.produceAndPersist() {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

  private fun createTemporaryAccommodationBooking(
    premises: TemporaryAccommodationPremisesEntity,
    offenderDetails: OffenderDetailSummary,
  ): BookingEntity {
    val room = roomEntityFactory.produceAndPersist() {
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
    GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

    webTestClient.post()
      .uri("/premises/${booking.premises.id}/bookings/${booking.id}/extensions")
      .header("Authorization", "Bearer $jwt")
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
