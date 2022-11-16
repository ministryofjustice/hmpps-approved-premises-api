package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApprovedPremisesBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTemporaryAccommodationBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
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
  fun `Get a booking for an Approved Premises returns OK with the correct body`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val keyWorker = ContextStaffMemberFactory().produce()
    mockStaffMembersContextApiCall(keyWorker, premises.qCode)

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withStaffKeyWorkerCode(keyWorker.code)
      withCrn("CRN123")
      withServiceName(ServiceName.approvedPremises)
    }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings/${booking.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          bookingTransformer.transformJpaToApi(booking, offenderDetails, inmateDetail, keyWorker)
        )
      )
  }

  @Test
  fun `Get a booking for an Temporary Accommodation Premises returns OK with the correct body`() {
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

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
      withCrn("CRN123")
      withServiceName(ServiceName.temporaryAccommodation)
      withYieldedBed { bed }
    }

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings/${booking.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          bookingTransformer.transformJpaToApi(booking, offenderDetails, inmateDetail, null)
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
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get all Bookings on Premises without any Bookings returns empty list`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } } }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val keyWorker = ContextStaffMemberFactory().produce()
    mockStaffMembersContextApiCall(keyWorker, premises.qCode)

    val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
      withPremises(premises)
      withStaffKeyWorkerCode(keyWorker.code)
      withCrn("CRN123")
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

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val expectedJson = objectMapper.writeValueAsString(
      bookings.map {
        bookingTransformer.transformJpaToApi(it, offenderDetails, inmateDetail, keyWorker)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Create Approved Premises Booking returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN321")
      .withFirstName("Mock")
      .withLastName("Person")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApprovedPremisesBooking(
          crn = "CRN321",
          arrivalDate = LocalDate.parse("2022-08-12"),
          departureDate = LocalDate.parse("2022-08-30"),
          serviceName = ServiceName.approvedPremises,
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.person.crn").isEqualTo("CRN321")
      .jsonPath("$.person.name").isEqualTo("Mock Person")
      .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
      .jsonPath("$.departureDate").isEqualTo("2022-08-30")
      .jsonPath("$.keyWorker").isEqualTo(null)
      .jsonPath("$.status").isEqualTo("awaiting-arrival")
      .jsonPath("$.arrival").isEqualTo(null)
      .jsonPath("$.departure").isEqualTo(null)
      .jsonPath("$.nonArrival").isEqualTo(null)
      .jsonPath("$.cancellation").isEqualTo(null)
      .jsonPath("$.serviceName").isEqualTo(ServiceName.approvedPremises.value)
      .jsonPath("$.bed.id").doesNotHaveJsonPath()
      .jsonPath("$.bed.name").doesNotHaveJsonPath()
  }

  @Test
  fun `Create Temporary Accommodation Booking returns OK with correct body`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
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

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN321")
      .withFirstName("Mock")
      .withLastName("Person")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewTemporaryAccommodationBooking(
          crn = "CRN321",
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
      .jsonPath("$.person.crn").isEqualTo("CRN321")
      .jsonPath("$.person.name").isEqualTo("Mock Person")
      .jsonPath("$.arrivalDate").isEqualTo("2022-08-12")
      .jsonPath("$.departureDate").isEqualTo("2022-08-30")
      .jsonPath("$.keyWorker").isEqualTo(null)
      .jsonPath("$.status").isEqualTo("awaiting-arrival")
      .jsonPath("$.arrival").isEqualTo(null)
      .jsonPath("$.departure").isEqualTo(null)
      .jsonPath("$.nonArrival").isEqualTo(null)
      .jsonPath("$.cancellation").isEqualTo(null)
      .jsonPath("$.serviceName").isEqualTo(ServiceName.temporaryAccommodation.value)
      .jsonPath("$.bed.id").isEqualTo(bed.id.toString())
      .jsonPath("$.bed.name").isEqualTo("test-bed")
  }

  @Test
  fun `Create Temporary Accommodation Booking returns 400 when bed does not exist on the premises`() {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN321")
      .withFirstName("Mock")
      .withLastName("Person")
      .withNomsNumber("NOMS321")
      .produce()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    webTestClient.post()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewTemporaryAccommodationBooking(
          crn = "CRN321",
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
  fun `Create Arrival on Booking returns 200 with correct body`() {
    val keyWorker = ContextStaffMemberFactory().produce()
    mockStaffMembersContextApiCall(keyWorker, "QCODE")

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

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Create Cancellation on Booking returns OK with correct body`() {
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

    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
  fun `Create Extension on Booking returns OK with expected body, updates departureDate on Booking entity`() {
    val keyWorker = ContextStaffMemberFactory().produce()
    mockStaffMembersContextApiCall(keyWorker, "QCODE")

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

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

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
}
