package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3.v2

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3BookingTransformer
import java.util.UUID

class Cas3v2BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: Cas3BookingTransformer

  lateinit var probationRegion: ProbationRegionEntity

  @BeforeEach
  fun before() {
    probationRegion = givenAProbationRegion()
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
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }
}
