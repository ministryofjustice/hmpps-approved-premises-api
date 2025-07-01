package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3PremisesTest : Cas3IntegrationTestBase() {
  @Autowired
  lateinit var cas3FutureBookingTransformer: Cas3FutureBookingTransformer

  @Nested
  inner class CreatePremises {
    @Test
    fun `Create new premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val characteristics = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("premises")
          withServiceScope(ServiceName.temporaryAccommodation.value)
          withName(randomStringMultiCaseWithNumbers(10))
        }

        val characteristicIds = characteristics.map { it.id }.sortedBy { it }

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = randomStringMultiCaseWithNumbers(12),
          town = randomStringMultiCaseWithNumbers(10),
          postcode = randomPostCode(),
          localAuthorityAreaId = null,
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = characteristicIds,
          notes = randomStringLowerCase(100),
          turnaroundWorkingDays = 3,
        )

        webTestClient.post()
          .uri("/cas3/premises")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newPremises.reference)
          .jsonPath("addressLine1").isEqualTo(newPremises.addressLine1)
          .jsonPath("addressLine2").isEqualTo(newPremises.addressLine2)
          .jsonPath("town").isEqualTo(newPremises.town)
          .jsonPath("postcode").isEqualTo(newPremises.postcode)
          .jsonPath("localAuthorityArea").isEmpty()
          .jsonPath("probationRegion.id").isEqualTo(newPremises.probationRegionId.toString())
          .jsonPath("probationDeliveryUnit.id").isEqualTo(newPremises.probationDeliveryUnitId.toString())
          .jsonPath("notes").isEqualTo(newPremises.notes)
          .jsonPath("turnaroundWorkingDays").isEqualTo(newPremises.turnaroundWorkingDays.toString())
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "premises" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
          .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })
      }
    }

    @Test
    fun `When a new premises is created with default values for optional properties returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = null,
          town = null,
          postcode = randomPostCode(),
          localAuthorityAreaId = null,
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = emptyList(),
          notes = null,
          turnaroundWorkingDays = null,
        )

        webTestClient.post()
          .uri("/cas3/premises")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newPremises.reference)
          .jsonPath("addressLine1").isEqualTo(newPremises.addressLine1)
          .jsonPath("addressLine2").isEmpty()
          .jsonPath("town").isEmpty()
          .jsonPath("postcode").isEqualTo(newPremises.postcode)
          .jsonPath("localAuthorityArea").isEmpty()
          .jsonPath("probationRegion.id").isEqualTo(newPremises.probationRegionId.toString())
          .jsonPath("probationDeliveryUnit.id").isEqualTo(newPremises.probationDeliveryUnitId.toString())
          .jsonPath("notes").isEmpty()
          .jsonPath("turnaroundWorkingDays").isEqualTo("2")
      }
    }

    @Test
    fun `Create new Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }
        val anotherProbationRegion = probationRegionEntityFactory.produceAndPersist()

        val newPremises = Cas3NewPremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          postcode = randomPostCode(),
          probationRegionId = anotherProbationRegion.id,
          probationDeliveryUnitId = pdu.id,
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/premises")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(newPremises)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

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
    fun `Get all Premises returns OK with correct premises sorted and containing online, upcoming and archived bedspaces`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, expectedPremisesSummaries) = getListPremises(user.probationRegion)

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

    @ParameterizedTest()
    @EnumSource(value = Cas3PremisesSortBy::class)
    fun `Get all Premises returns OK with correct premises sorted by PDU or LA`(sortBy: Cas3PremisesSortBy) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->

        val premisesSummary1 = getListPremisesWithIndividualPduAndLa(user.probationRegion, "PDU3", "LA1")
        val premisesSummary2 = getListPremisesWithIndividualPduAndLa(user.probationRegion, "PDU1", "LA2")
        val premisesSummary3 = getListPremisesWithIndividualPduAndLa(user.probationRegion, "PDU2", "LA3")

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?sortBy=${sortBy.name}",
          when (sortBy) {
            Cas3PremisesSortBy.pdu -> {
              mutableListOf(premisesSummary2, premisesSummary3, premisesSummary1)
            }
            Cas3PremisesSortBy.la -> {
              mutableListOf(premisesSummary1, premisesSummary2, premisesSummary3)
            }
          },
        )
      }
    }

    private fun getListPremisesWithIndividualPduAndLa(probationRegion: ProbationRegionEntity, pduName: String, laName: String): Cas3PremisesSummary {
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
        withProbationDeliveryUnit(
          probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(probationRegion)
            withName(pduName)
          },
        )
        withLocalAuthorityArea(
          localAuthorityEntityFactory.produceAndPersist {
            withName(laName)
          },
        )
        withStatus(PropertyStatus.active)
      }

      val onlineBedspacesSummary = createBedspaces(premises, Cas3BedspaceStatus.online, true)
      val upcomingBedspacesSummary = createBedspaces(premises, Cas3BedspaceStatus.upcoming, true)
      return createPremisesSummary(premises, (onlineBedspacesSummary.size + upcomingBedspacesSummary.size))
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
        val (premises, expectedPremisesSummaries) = getListPremises(user.probationRegion)

        // filter premises with full postcode
        val expectedPremisesPostcode = premises.take(2).first()

        val expectedPremisesSummaryPostcode = expectedPremisesSummaries.take(2).first()

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPostcode.postcode}",
          listOf(expectedPremisesSummaryPostcode),
        )

        // filter premises with full postcode without whitespaces
        val expectedPremisesPostcodeWithoutSpaces = premises.take(3).first()

        val expectedPremisesSummaryWithoutSpaces = expectedPremisesSummaries.take(3).first()

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPostcodeWithoutSpaces.postcode.replace(" ", "")}",
          listOf(expectedPremisesSummaryWithoutSpaces),
        )

        // filter premises with partial postcode
        val expectedPremisesPartialPostcode = premises.take(5).first()

        val expectedPremisesSummaryPartialPostcode = expectedPremisesSummaries.take(5).first()

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPartialPostcode.postcode.split(" ").first()}",
          listOf(expectedPremisesSummaryPartialPostcode),
        )

        // filter premises with partial postcode without whitespaces
        val expectedPremisesPartialPostcodeWithoutSpaces = premises.take(8).first()

        val expectedPremisesSummaryPartialPostcodeWithoutSpaces = expectedPremisesSummaries.take(8).first()

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=${expectedPremisesPartialPostcodeWithoutSpaces.postcode.replace(" ", "").substring(0, 5)}",
          listOf(expectedPremisesSummaryPartialPostcodeWithoutSpaces),
        )
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["addressLine1", "addressLine2"])
    fun `Get all premises filters correctly when a premises address is passed in the query parameter`(addressLineField: String) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val (premises, premisesSummaries) = getListPremises(user.probationRegion)

        // filter premises with the full premises address
        val expectedPremisesAddress = premises.take(6).first()

        var expectedPremisesSummaryAddress: List<Cas3PremisesSummary>

        val addressLine = when (addressLineField) {
          "addressLine1" -> {
            expectedPremisesSummaryAddress = premisesSummaries.filter { it.addressLine1 == expectedPremisesAddress.addressLine1 }
            expectedPremisesAddress.addressLine1
          }
          "addressLine2" -> {
            expectedPremisesSummaryAddress = premisesSummaries.filter { it.addressLine2 == expectedPremisesAddress.addressLine2 }
            expectedPremisesAddress.addressLine2
          }
          else -> error("unexpected value $addressLineField")
        }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=$addressLine",
          expectedPremisesSummaryAddress,
        )

        // filter premises with the partial premises address
        val expectedPremisesPartialAddress = premises.take(6).first()

        val partialAddressLineToSearchBy = when (addressLineField) {
          "addressLine1" -> {
            val partialAddressLine = expectedPremisesPartialAddress.addressLine1.split(" ").last()
            expectedPremisesSummaryAddress = premisesSummaries.filter { it.addressLine1.contains(partialAddressLine) }
            partialAddressLine
          }
          "addressLine2" -> {
            val partialAddressLine = expectedPremisesPartialAddress.addressLine2?.split(" ")?.last()
            expectedPremisesSummaryAddress = premisesSummaries.filter { it.addressLine2?.contains(partialAddressLine!!) == true }
            partialAddressLine
          }
          else -> error("unexpected value $addressLineField")
        }

        assertUrlReturnsPremises(
          jwt,
          "/cas3/premises/summary?postcodeOrAddress=$partialAddressLineToSearchBy",
          expectedPremisesSummaryAddress,
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

    private fun getListPremises(probationRegion: ProbationRegionEntity): Pair<List<TemporaryAccommodationPremisesEntity>, List<Cas3PremisesSummary>> {
      val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
      val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }
      val premisesSummary = mutableListOf<Cas3PremisesSummary>()
      val onlinePremisesWithBedspaceWithoutEndDate = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 5,
        propertyStatus = PropertyStatus.active,
      ).map { premises ->
        val onlineBedspaces = createBedspaces(premises, Cas3BedspaceStatus.online, true)
        val upcomingBedspaces = createBedspaces(premises, Cas3BedspaceStatus.upcoming, true)
        premisesSummary.add(createPremisesSummary(premises, (onlineBedspaces.size + upcomingBedspaces.size)))
        premises
      }

      val onlinePremisesWithBedspaceWithEndDate = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 5,
        propertyStatus = PropertyStatus.active,
      ).map { premises ->
        val onlineBedspaces = createBedspaces(premises, Cas3BedspaceStatus.online)
        val upcomingBedspaces = createBedspaces(premises, Cas3BedspaceStatus.upcoming)
        premisesSummary.add(createPremisesSummary(premises, (onlineBedspaces.size + upcomingBedspaces.size)))
        premises
      }

      val archivedPremises = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 3,
        propertyStatus = PropertyStatus.archived,
      ).map { premises ->
        createBedspaces(premises, Cas3BedspaceStatus.archived)
        premisesSummary.add(createPremisesSummary(premises, 0))
        premises
      }

      val allPremises = (onlinePremisesWithBedspaceWithoutEndDate + onlinePremisesWithBedspaceWithEndDate + archivedPremises)
      return Pair(allPremises.sortedBy { it.id }, premisesSummary.sortedBy { it.id })
    }

    private fun createBedspaces(premises: TemporaryAccommodationPremisesEntity, status: Cas3BedspaceStatus, withoutEndDate: Boolean = false): List<BedEntity> {
      var startDate = LocalDate.now().minusDays(30)
      var endDate: LocalDate? = null
      val bedspaces = mutableListOf<BedEntity>()

      repeat(randomInt(1, 5)) {
        when (status) {
          Cas3BedspaceStatus.online -> {
            startDate = LocalDate.now().randomDateBefore(360)
            endDate = when {
              withoutEndDate -> null
              else -> LocalDate.now().plusDays(1).randomDateAfter(90)
            }
          }
          Cas3BedspaceStatus.upcoming -> {
            startDate = LocalDate.now().plusDays(1).randomDateAfter(30)
            endDate = when {
              withoutEndDate -> null
              else -> startDate.plusDays(1).randomDateAfter(90)
            }
          }
          Cas3BedspaceStatus.archived -> {
            endDate = LocalDate.now().minusDays(1).randomDateBefore(360)
            startDate = endDate!!.randomDateBefore(360)
          }
        }

        bedspaces.add(createBedspaceInPremises(premises, startDate, endDate))
      }

      return bedspaces
    }

    private fun createPremisesSummary(premises: TemporaryAccommodationPremisesEntity, bedspaceCount: Int) = Cas3PremisesSummary(
      id = premises.id,
      name = premises.name,
      addressLine1 = premises.addressLine1,
      addressLine2 = premises.addressLine2,
      postcode = premises.postcode,
      pdu = premises.probationDeliveryUnit?.name!!,
      status = premises.status,
      bedspaceCount = bedspaceCount,
      localAuthorityAreaName = premises.localAuthorityArea?.name!!,
    )
  }

  @Nested
  inner class GetPremisesById {
    @Test
    fun `Get Premises by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val premises = getListPremisesByStatus(
          probationRegion = user.probationRegion,
          probationDeliveryUnit = probationDeliveryUnit,
          localAuthorityArea = localAuthorityArea,
          numberOfPremises = 5,
          propertyStatus = PropertyStatus.active,
        ).map { premises ->
          // online bedspaces
          createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)
          createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = LocalDate.now().plusDays(1).randomDateAfter(90))

          // upcoming bedspaces
          createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateAfter(30), endDate = null)

          // archived bedspaces
          createBedspaceInPremises(
            premises,
            startDate = LocalDate.now().minusDays(180).randomDateBefore(120),
            endDate = LocalDate.now().minusDays(1).randomDateBefore(90),
          )

          premises
        }

        val premisesToGet = premises.drop(1).first()

        val expectedPremises = Cas3Premises(
          id = premisesToGet.id,
          reference = premisesToGet.name,
          addressLine1 = premisesToGet.addressLine1,
          addressLine2 = premisesToGet.addressLine2,
          postcode = premisesToGet.postcode,
          town = premisesToGet.town,
          probationRegion = ProbationRegion(user.probationRegion.id, user.probationRegion.name),
          probationDeliveryUnit = ProbationDeliveryUnit(probationDeliveryUnit.id, probationDeliveryUnit.name),
          localAuthorityArea = LocalAuthorityArea(
            localAuthorityArea.id,
            localAuthorityArea.identifier,
            localAuthorityArea.name,
          ),
          startDate = premisesToGet.startDate,
          status = Cas3PremisesStatus.online,
          characteristics = premisesToGet.characteristics.sortedBy { it.id }.map { characteristic ->
            Characteristic(
              id = characteristic.id,
              name = characteristic.name,
              propertyName = characteristic.propertyName,
              serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
              modelScope = Characteristic.ModelScope.forValue(characteristic.modelScope),
            )
          },
          notes = premisesToGet.notes,
          turnaroundWorkingDays = premisesToGet.turnaroundWorkingDays,
          totalOnlineBedspaces = 2,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 1,
        )

        val responseBody = webTestClient.get()
          .uri("/cas3/premises/${premisesToGet.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedPremises))
      }
    }

    @Test
    fun `Get Premises by ID returns Not Found with correct body`() {
      val idToRequest = UUID.randomUUID().toString()

      val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

      webTestClient.get()
        .uri("/cas3/premises/$idToRequest")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectHeader().contentType("application/problem+json")
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("title").isEqualTo("Not Found")
        .jsonPath("status").isEqualTo(404)
        .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
    }

    @Test
    fun `Get Premises by ID for a premises not in the user's region returns 403 Forbidden`() {
      givenAUser { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        webTestClient.get()
          .uri("/cas3/premises/${premises.drop(1).first().id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
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

        val expectedBedspaces = mutableListOf<Cas3Bedspace>()

        premises.rooms.forEachIndexed { index, room ->
          val startDate = LocalDate.now().minusDays(index.toLong())
          val bedspace = bedEntityFactory.produceAndPersist {
            withRoom(room)
            withStartDate(startDate)
            withEndDate(startDate.plusDays(2))
          }.apply { room.beds.add(this) }

          if (bedspace.endDate!! > LocalDate.now()) {
            expectedBedspaces.add(createCas3Bedspoace(bedspace, room, Cas3BedspaceStatus.online))
          } else {
            expectedBedspaces.add(createCas3Bedspoace(bedspace, room, Cas3BedspaceStatus.archived))
          }
        }

        val roomWithoutEndDate = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        val bedspaceWithoutEndDate = bedEntityFactory.produceAndPersist {
          withRoom(roomWithoutEndDate)
          withStartDate(LocalDate.now().randomDateBefore(180))
        }.apply { roomWithoutEndDate.beds.add(this) }
        expectedBedspaces.add(createCas3Bedspoace(bedspaceWithoutEndDate, roomWithoutEndDate, Cas3BedspaceStatus.online))

        val roomWithUpcomingBedspace = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withBeds()
        }.apply { premises.rooms.add(this) }

        val upcomingBedspace = bedEntityFactory.produceAndPersist {
          withRoom(roomWithUpcomingBedspace)
          withStartDate(LocalDate.now().plusDays(10))
        }.apply { roomWithUpcomingBedspace.beds.add(this) }
        expectedBedspaces.add(createCas3Bedspoace(upcomingBedspace, roomWithUpcomingBedspace, Cas3BedspaceStatus.upcoming))

        val expectedCas3Bedspaces = Cas3Bedspaces(
          bedspaces = expectedBedspaces,
          totalOnlineBedspaces = 3,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 3,
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/premises/$premisesId/bedspaces",
          expectedCas3Bedspaces,
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
          withStartDate(LocalDate.now().randomDateBefore(320))
          withEndDate(LocalDate.now())
        }.apply { premises.rooms.first().beds.add(this) }

        val expectedBedspaces = Cas3Bedspaces(
          bedspaces = listOf(
            Cas3Bedspace(
              id = bed.id,
              reference = room1.name,
              startDate = bed.startDate,
              endDate = bed.endDate,
              status = Cas3BedspaceStatus.archived,
              characteristics = emptyList(),
              notes = room1.notes,
            ),
          ),
          totalOnlineBedspaces = 0,
          totalUpcomingBedspaces = 0,
          totalArchivedBedspaces = 1,
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
      expectedBedspaces: Cas3Bedspaces,
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

      assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedBedspaces))

      return response
    }

    private fun createCas3Bedspoace(bed: BedEntity, room: RoomEntity, bedspaceStatus: Cas3BedspaceStatus) = Cas3Bedspace(
      id = bed.id,
      reference = room.name,
      startDate = bed.startDate,
      characteristics = emptyList(),
      endDate = bed.endDate,
      status = bedspaceStatus,
      notes = room.notes,
    )
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
          status = Cas3BedspaceStatus.online,
          characteristics = listOf(
            Characteristic(
              id = roomCharacteristicOne.id,
              name = roomCharacteristicOne.name,
              propertyName = roomCharacteristicOne.propertyName,
              serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
              modelScope = Characteristic.ModelScope.forValue(roomCharacteristicOne.modelScope),
            ),
            Characteristic(
              id = roomCharacteristicTwo.id,
              name = roomCharacteristicTwo.name,
              propertyName = roomCharacteristicTwo.propertyName,
              serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
              modelScope = Characteristic.ModelScope.forValue(roomCharacteristicTwo.modelScope),
            ),
          ),
          notes = bedspace.room.notes,
        )

        val responseBody = webTestClient.get()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        assertThat(responseBody).isEqualTo(objectMapper.writeValueAsString(expectedBedspace))
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

  @Nested
  inner class CreateBedspace {
    @Test
    fun `Create new bedspace for Premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)
        val characteristics = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope(ServiceName.temporaryAccommodation.value)
          withName(randomStringMultiCaseWithNumbers(10))
        }

        val characteristicIds = characteristics.map { it.id }

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = characteristicIds,
          notes = randomStringLowerCase(100),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newBedspace.reference)
          .jsonPath("startDate").isEqualTo(newBedspace.startDate.toString())
          .jsonPath("notes").isEqualTo(newBedspace.notes)
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
          .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })
      }
    }

    @Test
    fun `When a new bedspace is created with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("notes").isEmpty()
      }
    }

    @Test
    fun `When create a new bedspace without a reference returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.reference")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `When create a new bedspace with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(7),
          startDate = LocalDate.now(),
          characteristicIds = mutableListOf(UUID.randomUUID()),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `When create a new bedspace with a characteristic of the wrong service scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope(ServiceName.approvedPremises.value)
        }.id

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(7),
          startDate = LocalDate.now(),
          characteristicIds = mutableListOf(characteristicId),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `When create a new bedspace with a characteristic of the wrong model scope returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }.id

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(7),
          startDate = LocalDate.now(),
          characteristicIds = mutableListOf(characteristicId),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }

    @Test
    fun `Create new bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(probationRegionEntityFactory.produceAndPersist())

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class UpdateBedspace {
    @Test
    fun `When updating a bedspace returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)

        val characteristics = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope(ServiceName.temporaryAccommodation.value)
          withName(randomStringMultiCaseWithNumbers(10))
        }

        val characteristicIds = characteristics.map { it.id }

        val updateBedspace = Cas3UpdateBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          characteristicIds = characteristicIds,
          notes = randomStringMultiCaseWithNumbers(30),
        )

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(updateBedspace)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("reference").isEqualTo(updateBedspace.reference)
          .jsonPath("notes").isEqualTo(updateBedspace.notes)
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
          .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })
      }
    }

    @Test
    fun `When updating a bedspace without notes it will default to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3UpdateBedspace(
              reference = randomStringMultiCaseWithNumbers(10),
              notes = null,
              characteristicIds = emptyList(),
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("notes").isEmpty()
      }
    }

    @Test
    fun `When updating a bedspace with empty reference returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = LocalDate.now().randomDateAfter(90))

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3UpdateBedspace(
              reference = "",
              notes = randomStringMultiCaseWithNumbers(120),
              characteristicIds = emptyList(),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("\$.reference")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `When updating a bedspace with an unknown characteristic returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3UpdateBedspace(
              reference = randomStringMultiCaseWithNumbers(12),
              notes = randomStringMultiCaseWithNumbers(120),
              characteristicIds = listOf(UUID.randomUUID()),
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

    @Test
    fun `When updating a bedspace with a characteristic that has an incorrect service scope returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("room")
          withServiceScope(ServiceName.approvedPremises.value)
        }.id

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3UpdateBedspace(
              reference = randomStringMultiCaseWithNumbers(12),
              notes = randomStringMultiCaseWithNumbers(120),
              characteristicIds = listOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicServiceScope")
      }
    }

    @Test
    fun `When updating a bedspace with a characteristic that has an incorrect model scope returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateBefore(360), endDate = null)

        val characteristicId = characteristicEntityFactory.produceAndPersist {
          withModelScope("premises")
          withServiceScope(ServiceName.temporaryAccommodation.value)
        }.id

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            Cas3UpdateBedspace(
              reference = randomStringMultiCaseWithNumbers(12),
              notes = randomStringMultiCaseWithNumbers(120),
              characteristicIds = listOf(characteristicId),
            ),
          )
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].errorType").isEqualTo("incorrectCharacteristicModelScope")
      }
    }
  }

  private fun createPremises(probationRegion: ProbationRegionEntity): TemporaryAccommodationPremisesEntity = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withYieldedProbationRegion { probationRegion }
  }
}
