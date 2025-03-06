package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var cas3FutureBookingTransformer: Cas3FutureBookingTransformer

  @Nested
  inner class GetFutureBookings {
    @Test
    fun `Get future bookings without JWT returns 401`() {
      webTestClient.get()
        .uri("cas3/premises/${UUID.randomUUID()}/future-bookings")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get future bookings for premises out of user region returns 403`() {
      givenAUser { user, jwt ->
        val probationRegion = probationRegionEntityFactory.produceAndPersist()
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Get future bookings for non existing premises returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premisesId = UUID.randomUUID()

        webTestClient.get()
          .uri("cas3/premises/$premisesId/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $premisesId could be found")
      }
    }

    @Test
    fun `Get future bookings returns OK with empty booking list if there are no future bookings`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().minusDays(60))
          withDepartureDate(LocalDate.now().minusDays(3))
        }

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              listOf<FutureBooking>(),
            ),
          )
      }
    }

    @Test
    fun `Get future bookings returns OK with expected future bookings`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
          withLocalAuthorityArea(localAuthorityArea)
        }

        val offenderCaseSummary = CaseSummaryFactory()
          .withCurrentExclusion(false)
          .withCurrentRestriction(false)
          .produce()

        // future provisional booking
        val provisionalBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().plusDays(1))
          withDepartureDate(LocalDate.now().plusDays(60))
        }

        // future confirmed booking
        val confirmedBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.confirmed)
          withArrivalDate(LocalDate.now().plusDays(10))
          withDepartureDate(LocalDate.now().plusDays(43))
        }

        confirmedBooking.confirmation = confirmationEntityFactory.produceAndPersist {
          withBooking(confirmedBooking)
        }

        // arrived booking with future departure date
        val arrivedBooking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderCaseSummary.crn)
          withStatus(BookingStatus.arrived)
          withArrivalDate(LocalDate.now().minusDays(10))
          withDepartureDate(LocalDate.now().plusDays(22))
        }

        arrivedBooking.arrivals = listOf(
          arrivalEntityFactory.produceAndPersist {
            withBooking(arrivedBooking)
            withArrivalDate(LocalDate.now().minusDays(10))
          },
        ).toMutableList()

        // provisional booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().minusDays(19))
          withDepartureDate(LocalDate.now().minusDays(7))
        }

        // cancelled booking
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.cancelled)
          withArrivalDate(LocalDate.now().plusDays(7))
          withDepartureDate(LocalDate.now().plusDays(72))
        }

        // departed booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.departed)
          withArrivalDate(LocalDate.now().minusDays(30))
          withDepartureDate(LocalDate.now())
        }

        // confirmed booking in the past
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.confirmed)
          withArrivalDate(LocalDate.now().minusDays(40))
          withDepartureDate(LocalDate.now().minusDays(10))
        }

        val expectedJson = objectMapper.writeValueAsString(
          listOf(provisionalBooking, confirmedBooking, arrivedBooking).map {
            cas3FutureBookingTransformer.transformJpaToApi(
              it,
              PersonSummaryInfoResult.Success.Full(it.crn, offenderCaseSummary),
            )
          },
        )

        apDeliusContextAddCaseSummaryToBulkResponse(offenderCaseSummary)

        apDeliusContextAddResponseToUserAccessCall(
          listOf(
            CaseAccessFactory()
              .withCrn(offenderCaseSummary.crn)
              .produce(),
          ),
          user.deliusUsername,
        )

        webTestClient.get()
          .uri("cas3/premises/${premises.id}/future-bookings?statuses=provisional,confirmed,arrived")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(expectedJson)
      }
    }
  }

  @Nested
  inner class GetPremisesSummary {
    @Test
    fun `Get all Premises returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getPremises(user.probationRegion)

        val expectedPremisesSummaries = premises.map { premisesSummaryTransformer(it) }.sortedBy { it.id.toString() }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary",
          expectedPremisesSummaries,
        )
      }
    }

    @Test
    fun `Get all premises filters correctly when a postcode is passed in the query parameter`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getPremises(user.probationRegion)

        // filter premises with full postcode
        val expectedPremisesPostcode = premises.take(2).first()

        val expectedPremisesSummaryPostcode = premisesSummaryTransformer(expectedPremisesPostcode)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPostcode.postcode}",
          listOf(expectedPremisesSummaryPostcode),
        )

        // filter premises with full postcode without whitespaces
        val expectedPremisesPostcodeWithoutSpaces = premises.take(3).first()

        val expectedPremisesSummaryWithoutSpaces = premisesSummaryTransformer(expectedPremisesPostcode)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPostcodeWithoutSpaces.postcode.replace(" ", "")}",
          listOf(expectedPremisesSummaryWithoutSpaces),
        )

        // filter premises with partial postcode
        val expectedPremisesPartialPostcode = premises.take(5).first()

        val expectedPremisesSummaryPartialPostcode = premisesSummaryTransformer(expectedPremisesPartialPostcode)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPartialPostcode.postcode.split(" ").first()}",
          listOf(expectedPremisesSummaryPartialPostcode),
        )

        // filter premises with partial postcode without whitespaces
        val expectedPremisesPartialPostcodeWithoutSpaces = premises.take(8).first()

        val expectedPremisesSummaryPartialPostcodeWithoutSpaces = premisesSummaryTransformer(expectedPremisesPartialPostcodeWithoutSpaces)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesSummaryPartialPostcodeWithoutSpaces.postcode.replace(" ", "").substring(0, 5)}",
          listOf(expectedPremisesSummaryPartialPostcodeWithoutSpaces),
        )
      }
    }

    @Test
    fun `Get all premises filters correctly when a premises address is passed in the query parameter`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getPremises(user.probationRegion)

        // filter premises with the full premises address
        val expectedPremisesAddress = premises.take(6).first()

        val expectedPremisesSummaryAddress = premisesSummaryTransformer(expectedPremisesAddress)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesAddress.addressLine1}",
          listOf(expectedPremisesSummaryAddress),
        )

        // filter premises with the partial premises address
        val expectedPremisesPartialAddress = premises.take(6).first()

        val expectedPremisesSummaryPartialAddress = premisesSummaryTransformer(expectedPremisesPartialAddress)

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPartialAddress.addressLine1.split(" ").last()}",
          listOf(expectedPremisesSummaryPartialAddress),
        )
      }
    }

    @Test
    fun `Get all premises returns successfully with no premises when a postcode or address is passed in the query parameter and doesn't match any premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${randomStringMultiCaseWithNumbers(10)}",
          emptyList(),
        )
      }
    }

    private fun assertUrlReturnsPremises(
      jwt: String,
      url: String,
      expectedPremisesSummaries: List<Cas3PremisesSummary>,
    ): WebTestClient.ResponseSpec {
      val response = webTestClient.get()
        .uri(url)
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk

      val responseBody = response
        .returnResult<String>()
        .responseBody
        .blockFirst()

      assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedPremisesSummaries))

      return response
    }

    private fun getPremises(probationRegion: ProbationRegionEntity): List<TemporaryAccommodationPremisesEntity> {
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(10) {
        withProbationRegion(probationRegion)
        withProbationDeliveryUnit(probationDeliveryUnit)
        withLocalAuthorityArea(localAuthorityArea)
      }

      premises.forEach { premises ->
        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        bedEntityFactory.produceAndPersist {
          withRoom(room)
        }.apply { premises.rooms.first().beds.add(this) }
      }

      return premises
    }

    private fun premisesSummaryTransformer(premises: TemporaryAccommodationPremisesEntity) = Cas3PremisesSummary(
      id = premises.id,
      name = premises.name,
      addressLine1 = premises.addressLine1,
      addressLine2 = premises.addressLine2,
      postcode = premises.postcode,
      pdu = premises.probationDeliveryUnit?.name!!,
      status = premises.status,
      bedspaceCount = premises.rooms.flatMap { it.beds }.size,
      localAuthorityAreaName = premises.localAuthorityArea?.name!!,
    )
  }
}
