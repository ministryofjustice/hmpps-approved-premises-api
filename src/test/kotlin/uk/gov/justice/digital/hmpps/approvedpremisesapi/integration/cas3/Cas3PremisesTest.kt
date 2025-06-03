package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesTest : Cas3IntegrationTestBase() {
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
      givenAUser { _, jwt ->
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

        confirmedBooking.confirmation = cas3ConfirmationEntityFactory.produceAndPersist {
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
    fun `Get all Premises returns OK with correct premises sorted and containing archived and online bedspaces`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getListPremises(user.probationRegion)

        val expectedPremisesSummaries = premises.map { premisesSummaryTransformer(it) }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary",
          expectedPremisesSummaries,
        )
      }
    }

    @Test
    fun `Get all Premises returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val expectedPremises = getPremises(user.probationRegion)

        // unexpectedCas3Premises that's in a different region
        temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { expectedPremises }
        }

        bedEntityFactory.produceAndPersistMultiple(5) {
          withYieldedRoom { room }
        }

        webTestClient.get()
          .uri("/cas3/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(expectedPremises.id.toString())
          .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
          .jsonPath("$[0].addressLine2").isEqualTo("221B")
          .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
          .jsonPath("$[0].status").isEqualTo("active")
          .jsonPath("$[0].pdu").isEqualTo(expectedPremises.probationDeliveryUnit!!.name)
          .jsonPath("$[0].localAuthorityAreaName").isEqualTo(expectedPremises.localAuthorityArea!!.name)
          .jsonPath("$[0].bedspaceCount").isEqualTo(5)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @Test
    fun `Get all Premises returns premises with all bedspaces are archived`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val expectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(user.probationRegion)
          withId(UUID.randomUUID())
          withAddressLine1("8 Knox Street")
          withAddressLine2("Flat 1")
          withPostcode("W1H 1FY")
          withStatus(PropertyStatus.archived)
          withYieldedProbationDeliveryUnit {
            probationDeliveryUnitFactory.produceAndPersist {
              withProbationRegion(user.probationRegion)
            }
          }
          withService("CAS3")
        }

        val roomPremises1 = roomEntityFactory.produceAndPersist {
          withYieldedPremises { expectedPremises }
        }

        bedEntityFactory.produceAndPersist {
          withYieldedRoom { roomPremises1 }
          withEndDate { LocalDate.now() }
        }

        val roomPremises2 = roomEntityFactory.produceAndPersist {
          withYieldedPremises { expectedPremises }
        }

        bedEntityFactory.produceAndPersist {
          withYieldedRoom { roomPremises2 }
          withEndDate { LocalDate.parse("2024-01-13") }
        }

        webTestClient.get()
          .uri("/cas3/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].addressLine1").isEqualTo("8 Knox Street")
          .jsonPath("$[0].addressLine2").isEqualTo("Flat 1")
          .jsonPath("$[0].postcode").isEqualTo("W1H 1FY")
          .jsonPath("$[0].status").isEqualTo("archived")
          .jsonPath("$[0].pdu").isEqualTo(expectedPremises.probationDeliveryUnit!!.name)
          .jsonPath("$[0].localAuthorityAreaName").isEqualTo(expectedPremises.localAuthorityArea!!.name)
          .jsonPath("$[0].bedspaceCount").isEqualTo(0)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @Test
    fun `Get all Premises returns premises without bedspaces`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val expectedPremises = getPremises(user.probationRegion)

        webTestClient.get()
          .uri("/cas3/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(expectedPremises.id.toString())
          .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
          .jsonPath("$[0].addressLine2").isEqualTo("221B")
          .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
          .jsonPath("$[0].status").isEqualTo("active")
          .jsonPath("$[0].pdu").isEqualTo(expectedPremises.probationDeliveryUnit!!.name)
          .jsonPath("$[0].localAuthorityAreaName").isEqualTo(expectedPremises.localAuthorityArea!!.name)
          .jsonPath("$[0].bedspaceCount").isEqualTo(0)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @Test
    fun `Get all Premises returns bedspace count as expected when there is an archived bedspace`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val expectedPremises = getPremises(user.probationRegion)

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { expectedPremises }
        }

        bedEntityFactory.produceAndPersistMultiple(3) {
          withYieldedRoom { room }
        }

        bedEntityFactory.produceAndPersistMultiple(1) {
          withYieldedRoom { room }
          withEndDate { LocalDate.now().minusWeeks(1) }
        }

        webTestClient.get()
          .uri("/cas3/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(expectedPremises.id.toString())
          .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
          .jsonPath("$[0].addressLine2").isEqualTo("221B")
          .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
          .jsonPath("$[0].status").isEqualTo("active")
          .jsonPath("$[0].pdu").isEqualTo(expectedPremises.probationDeliveryUnit!!.name)
          .jsonPath("$[0].bedspaceCount").isEqualTo(3)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @Test
    fun `Get all Premises returns a bedspace count as expected when beds are active`() {
      givenAUser { user, jwt ->
        val expectedPremises = getPremises(user.probationRegion)

        val room = roomEntityFactory.produceAndPersist {
          withYieldedPremises { expectedPremises }
        }

        bedEntityFactory.produceAndPersistMultiple(3) {
          withYieldedRoom { room }
        }

        bedEntityFactory.produceAndPersistMultiple(1) {
          withYieldedRoom { room }
          withEndDate { LocalDate.now().plusWeeks(1) }
        }

        webTestClient.get()
          .uri("/cas3/premises/summary")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].id").isEqualTo(expectedPremises.id.toString())
          .jsonPath("$[0].addressLine1").isEqualTo("221 Baker Street")
          .jsonPath("$[0].addressLine2").isEqualTo("221B")
          .jsonPath("$[0].postcode").isEqualTo("NW1 6XE")
          .jsonPath("$[0].status").isEqualTo("active")
          .jsonPath("$[0].pdu").isEqualTo(expectedPremises.probationDeliveryUnit!!.name)
          .jsonPath("$[0].bedspaceCount").isEqualTo(4)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @Test
    fun `Get all premises filters correctly when a postcode is passed in the query parameter`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getListPremises(user.probationRegion)

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
    fun `Get all premises filters correctly when 'archived' is passed in to the query parameter`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getListPremises(user.probationRegion)

        val expectedPremisesSummaryArchived = premises.filter { it.status == PropertyStatus.archived }.map { premisesSummaryTransformer(it) }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?propertyStatus=${Cas3PropertyStatus.archived}",
          expectedPremisesSummaryArchived,
        )
      }
    }

    @Test
    fun `Get all premises filters correctly when 'online' is passed in to the query parameter`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getListPremises(user.probationRegion)

        val expectedPremisesSummaryOnline = premises.filter { it.status == PropertyStatus.active }.map { premisesSummaryTransformer(it) }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?propertyStatus=${Cas3PropertyStatus.online}",
          expectedPremisesSummaryOnline,
        )
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["addressLine1", "addressLine2"])
    fun `Get all premises filters correctly when a premises address is passed in the query parameter`(addressLineField: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = getListPremises(user.probationRegion)

        // filter premises with the full premises address
        val expectedPremisesAddress = premises.take(6).first()

        val expectedPremisesSummaryAddress = premisesSummaryTransformer(expectedPremisesAddress)

        val addressLine = when (addressLineField) {
          "addressLine1" -> expectedPremisesAddress.addressLine1
          "addressLine2" -> expectedPremisesAddress.addressLine2
          else -> error("unexpected value $addressLineField")
        }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=$addressLine",
          listOf(expectedPremisesSummaryAddress),
        )

        // filter premises with the partial premises address
        val expectedPremisesPartialAddress = premises.take(6).first()

        val expectedPremisesSummaryPartialAddress = premisesSummaryTransformer(expectedPremisesPartialAddress)

        val partialAddressLine = when (addressLineField) {
          "addressLine1" -> expectedPremisesPartialAddress.addressLine1
          "addressLine2" -> expectedPremisesPartialAddress.addressLine2!!
          else -> error("unexpected value $addressLineField")
        }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${partialAddressLine.split(" ").last()}",
          listOf(expectedPremisesSummaryPartialAddress),
        )
      }
    }

    @Test
    fun `Get all premises returns successfully with no premises when a postcode or address is passed in the query parameter and doesn't match any premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
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

    private fun getPremises(probationRegion: ProbationRegionEntity) = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withProbationRegion(probationRegion)
      withId(UUID.randomUUID())
      withAddressLine1("221 Baker Street")
      withAddressLine2("221B")
      withPostcode("NW1 6XE")
      withStatus(PropertyStatus.active)
      withYieldedProbationDeliveryUnit {
        probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        }
      }
      withService("CAS3")
    }

    private fun getListPremises(probationRegion: ProbationRegionEntity): List<TemporaryAccommodationPremisesEntity> {
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
      val premises = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        dbSize = 8,
        propertyStatus = PropertyStatus.pending,
      )
      val definitelyArchivedPremises = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        dbSize = 1,
        propertyStatus = PropertyStatus.archived,
      )
      val definitelyActivePremises = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        dbSize = 1,
        propertyStatus = PropertyStatus.active,
      )
      return (
        createRoomsWithSingleBedInPremises(premises = premises) +
          createRoomsWithSingleBedInPremises(premises = definitelyArchivedPremises, endDate = LocalDate.now().minusDays(4)) +
          createRoomsWithSingleBedInPremises(premises = definitelyActivePremises, endDate = LocalDate.now().plusDays(4))
        ).sortedBy { it.id }
    }

    private fun getListPremisesByStatus(
      probationRegion: ProbationRegionEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      dbSize: Int,
      propertyStatus: PropertyStatus,
    ): List<TemporaryAccommodationPremisesEntity> {
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(dbSize) {
        withProbationRegion(probationRegion)
        withProbationDeliveryUnit(probationDeliveryUnit)
        withLocalAuthorityArea(localAuthorityArea)
        withStatus(propertyStatus)
      }

      return premises
    }

    private fun premisesSummaryTransformer(premises: TemporaryAccommodationPremisesEntity): Cas3PremisesSummary {
      val bedspaces = premises.rooms.map {
        Cas3BedspaceSummary(
          it.beds.first().id,
          it.name,
          if (it.beds.first().endDate == null || it.beds.first().endDate!! > LocalDate.now()) {
            Cas3BedspaceSummary.Status.online
          } else {
            Cas3BedspaceSummary.Status.archived
          },
        )
      }

      return Cas3PremisesSummary(
        id = premises.id,
        name = premises.name,
        addressLine1 = premises.addressLine1,
        addressLine2 = premises.addressLine2,
        postcode = premises.postcode,
        pdu = premises.probationDeliveryUnit?.name!!,
        status = premises.status,
        bedspaces = bedspaces,
        bedspaceCount = bedspaces.filter { it.status == Cas3BedspaceSummary.Status.online }.size,
        localAuthorityAreaName = premises.localAuthorityArea?.name!!,
      )
    }
  }

  @Nested
  inner class GetPremisesBedspaces {
    @Test
    fun `Get all Bedspaces returns OK with correct bedspaces sorted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premisesId = UUID.randomUUID()

        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withId(premisesId)
          withProbationRegion(user.probationRegion)
          withProbationDeliveryUnit(probationDeliveryUnit)
          withLocalAuthorityArea(localAuthorityArea)
        }

        roomEntityFactory.produceAndPersistMultiple(5) {
          withYieldedPremises { premises }
        }.apply { premises.rooms.addAll(this) }

        premises.rooms.forEachIndexed { index, room ->
          val startDate = LocalDate.now().minusDays(index.toLong())
          bedEntityFactory.produceAndPersist {
            withRoom(room)
            withStartDate(startDate)
            withEndDate(startDate.plusDays(2))
          }.apply { room.beds.add(this) }
        }

        val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        bedEntityFactory.produceAndPersist {
          withRoom(roomWithoutEndDate)
          withStartDate(LocalDate.now())
        }.apply { roomWithoutEndDate.beds.add(this) }

        val expectedBedspaces = premises.rooms.map { room ->
          val bed = room.beds.first()
          Cas3Bedspace(
            id = bed.id,
            reference = room.name,
            startDate = bed.startDate,
            characteristics = emptyList(),
            endDate = bed.endDate,
            notes = room.notes,
          )
        }

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/premises/$premisesId/bedspaces",
          expectedBedspaces,
        )
      }
    }

    @Test
    fun `Get all Bedspaces returns OK with rooms with no beds filtered out`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premisesId = UUID.randomUUID()

        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withId(premisesId)
          withProbationRegion(user.probationRegion)
          withProbationDeliveryUnit(probationDeliveryUnit)
          withLocalAuthorityArea(localAuthorityArea)
        }

        val room1 = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room1)
          withEndDate(LocalDate.now())
        }.apply { premises.rooms.first().beds.add(this) }

        val expectedBedspaces = listOf(
          Cas3Bedspace(
            id = bed.id,
            reference = room1.name,
            startDate = bed.startDate,
            characteristics = emptyList(),
            endDate = bed.endDate,
            notes = room1.notes,
          ),
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/premises/$premisesId/bedspaces",
          expectedBedspaces,
        )
      }
    }

    @Test
    fun `Get Bedspaces by ID returns Not Found with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->

        val premisesId = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/cas3/premises/$premisesId/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Premises with an ID of $premisesId could be found")
      }
    }

    @Test
    fun `Trying to get bedspaces the user is not authorized to view should return 403`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt1 ->
        givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { user2, _ ->

          val premisesId = UUID.randomUUID()

          val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
          val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(user2.probationRegion)
          }

          temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withId(premisesId)
            withProbationRegion(user2.probationRegion)
            withProbationDeliveryUnit(probationDeliveryUnit)
            withLocalAuthorityArea(localAuthorityArea)
          }

          webTestClient.get()
            .uri("/cas3/premises/$premisesId/bedspaces")
            .header("Authorization", "Bearer $jwt1")
            .exchange()
            .expectHeader().contentType("application/problem+json")
            .expectStatus()
            .isForbidden
            .expectBody()
            .jsonPath("title").isEqualTo("Forbidden")
            .jsonPath("status").isEqualTo(403)
            .jsonPath("detail").isEqualTo("You are not authorized to access this endpoint")
        }
      }
    }

    private fun assertUrlReturnsBedspaces(
      jwt: String,
      url: String,
      expectedPremisesSummaries: List<Cas3Bedspace>,
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
  }

  @Nested
  inner class GetBedspace {

    @Test
    fun `Get Bedspace by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->

        val roomCharacteristicOne = produceCharacteristic("CharacteristicOne", Characteristic.ModelScope.room)
        val roomCharacteristicTwo = produceCharacteristic("CharacteristicTwo", Characteristic.ModelScope.room)

        val (premises, bedspace) = createPremisesAndBedspace(
          user.probationRegion,
          listOf(roomCharacteristicOne, roomCharacteristicTwo),
        )

        val expectedBedspace = Cas3Bedspace(
          id = bedspace.id,
          reference = bedspace.room.name,
          startDate = bedspace.startDate,
          endDate = bedspace.endDate,
          characteristics = listOf(
            Characteristic(
              id = roomCharacteristicOne.id,
              name = roomCharacteristicOne.name,
              propertyName = roomCharacteristicOne.propertyName,
              serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
              modelScope = Characteristic.ModelScope.room,
            ),
            Characteristic(
              id = roomCharacteristicTwo.id,
              name = roomCharacteristicTwo.name,
              propertyName = roomCharacteristicTwo.propertyName,
              serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
              modelScope = Characteristic.ModelScope.room,
            ),
          ),
          notes = bedspace.room.notes,
        )

        val responseBosy = webTestClient.get()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        assertThat(responseBosy).isEqualTo(objectMapper.writeValueAsString(expectedBedspace))
      }
    }

    @Test
    fun `Get Bedspace by ID returns Not Found with correct body when Premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = createPremisesAndBedspace(user.probationRegion, emptyList())

        val unexistPremisesId = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/cas3/premises/$unexistPremisesId/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Premises with an ID of $unexistPremisesId could be found")
      }
    }

    @Test
    fun `Get Bedspace by ID returns Not Found with correct body when Bedspace does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, bedspace) = createPremisesAndBedspace(user.probationRegion, emptyList())

        val unexistBedspaceId = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/cas3/premises/${premises.id}/bedspaces/$unexistBedspaceId")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No Bedspace with an ID of $unexistBedspaceId could be found")
      }
    }

    @Test
    fun `Get Bedspace by ID for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser { _, jwt ->
        val otherRegion = givenAProbationRegion()
        val (premises, bedspace) = createPremisesAndBedspace(otherRegion, emptyList())

        webTestClient.get()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    private fun createPremisesAndBedspace(probationRegion: ProbationRegionEntity, roomCharacteristics: List<CharacteristicEntity>): Pair<TemporaryAccommodationPremisesEntity, BedEntity> {
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        withProbationRegion(probationRegion)
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
        withName(randomStringMultiCaseWithNumbers(10))
        withNotes(randomStringLowerCase(100))
        withCharacteristicsList(roomCharacteristics)
      }

      val bedspace = bedEntityFactory.produceAndPersist {
        withStartDate(LocalDate.now().minusDays(120))
        withRoom(room)
      }

      return Pair(premises, bedspace)
    }

    private fun produceCharacteristic(
      propertyName: String,
      modelScope: Characteristic.ModelScope,
    ): CharacteristicEntity {
      val characteristicTwo = characteristicRepository.save(
        CharacteristicEntityFactory()
          .withPropertyName(propertyName)
          .withServiceScope(ServiceName.temporaryAccommodation.value)
          .withModelScope(modelScope.value)
          .produce(),
      )
      return characteristicTwo
    }
  }
}
