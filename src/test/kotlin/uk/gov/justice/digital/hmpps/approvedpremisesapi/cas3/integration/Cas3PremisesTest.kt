package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremisesComplete
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremisesWithRoomsAndBeds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationPremisesWithUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenATemporaryAccommodationRooms
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ArchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSortBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class Cas3PremisesTest : Cas3IntegrationTestBase() {
  @Autowired
  private lateinit var premisesRepository: PremisesRepository

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
  inner class UpdatePremises {
    @Test
    fun `Update premises returns 200 OK with correct body`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { user, jwt, premises ->

        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val updatedPremises = Cas3UpdatePremises(
          reference = randomStringMultiCaseWithNumbers(10),
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = randomStringMultiCaseWithNumbers(10),
          postcode = randomPostCode(),
          town = randomNumberChars(10),
          notes = randomStringMultiCaseWithNumbers(100),
          probationRegionId = premises.probationRegion.id,
          probationDeliveryUnitId = probationDeliveryUnit.id,
          localAuthorityAreaId = premises.localAuthorityArea?.id!!,
          characteristicIds = premises.characteristics.sortedBy { it.id }.map { characteristic ->
            characteristic.id
          },
          turnaroundWorkingDayCount = 3,
        )

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(updatedPremises)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("reference").isEqualTo(updatedPremises.reference)
          .jsonPath("addressLine1").isEqualTo(updatedPremises.addressLine1)
          .jsonPath("addressLine2").isEqualTo(updatedPremises.addressLine2!!)
          .jsonPath("town").isEqualTo(updatedPremises.town!!)
          .jsonPath("postcode").isEqualTo(updatedPremises.postcode)
          .jsonPath("localAuthorityArea.id").isEqualTo(updatedPremises.localAuthorityAreaId.toString())
          .jsonPath("probationRegion.id").isEqualTo(updatedPremises.probationRegionId.toString())
          .jsonPath("probationDeliveryUnit.id").isEqualTo(updatedPremises.probationDeliveryUnitId.toString())
          .jsonPath("notes").isEqualTo(updatedPremises.notes!!)
          .jsonPath("turnaroundWorkingDays").isEqualTo(3)
          .jsonPath("characteristics[*].id").isEqualTo(updatedPremises.characteristicIds.map { it.toString() })
      }
    }

    @Test
    fun `Update premises returns 200 OK with correct body when the reference hasn't been changed`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { user, jwt, premises ->

        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val updatedPremises = Cas3UpdatePremises(
          reference = premises.name,
          addressLine1 = randomStringMultiCaseWithNumbers(25),
          addressLine2 = randomStringMultiCaseWithNumbers(10),
          postcode = randomPostCode(),
          town = randomNumberChars(10),
          notes = randomStringMultiCaseWithNumbers(100),
          probationRegionId = premises.probationRegion.id,
          probationDeliveryUnitId = probationDeliveryUnit.id,
          localAuthorityAreaId = premises.localAuthorityArea?.id!!,
          characteristicIds = premises.characteristics.sortedBy { it.id }.map { characteristic ->
            characteristic.id
          },
          turnaroundWorkingDayCount = 3,
        )

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(updatedPremises)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("reference").isEqualTo(updatedPremises.reference)
          .jsonPath("addressLine1").isEqualTo(updatedPremises.addressLine1)
          .jsonPath("addressLine2").isEqualTo(updatedPremises.addressLine2!!)
          .jsonPath("town").isEqualTo(updatedPremises.town!!)
          .jsonPath("postcode").isEqualTo(updatedPremises.postcode)
          .jsonPath("localAuthorityArea.id").isEqualTo(updatedPremises.localAuthorityAreaId.toString())
          .jsonPath("probationRegion.id").isEqualTo(updatedPremises.probationRegionId.toString())
          .jsonPath("probationDeliveryUnit.id").isEqualTo(updatedPremises.probationDeliveryUnitId.toString())
          .jsonPath("notes").isEqualTo(updatedPremises.notes!!)
          .jsonPath("turnaroundWorkingDays").isEqualTo(3)
          .jsonPath("characteristics[*].id").isEqualTo(updatedPremises.characteristicIds.map { it.toString() })
      }
    }

    @Test
    fun `Update premises returns 403 Forbidden when user access is not allowed as they are out of region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val probationRegion = probationRegionEntityFactory.produceAndPersist()
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(probationRegion)
        }

        val premisesCharacteristics = getPremisesCharacteristics().toMutableList()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withProbationDeliveryUnit(probationDeliveryUnit)
          withLocalAuthorityArea(localAuthorityArea)
          withStatus(PropertyStatus.active)
          withAddressLine2(randomStringUpperCase(10))
          withCharacteristics(
            mutableListOf(
              pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
              pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
              pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
            ),
          )
        }

        val updatedPremises = Cas3UpdatePremises(
          reference = premises.name,
          addressLine1 = premises.addressLine1,
          addressLine2 = premises.addressLine2,
          postcode = premises.postcode,
          town = premises.town,
          notes = premises.notes,
          probationRegionId = premises.probationRegion.id,
          probationDeliveryUnitId = premises.probationDeliveryUnit?.id!!,
          localAuthorityAreaId = premises.localAuthorityArea?.id!!,
          characteristicIds = premises.characteristics.sortedBy { it.id }.map { characteristic ->
            characteristic.id
          },
          turnaroundWorkingDayCount = premises.turnaroundWorkingDays,
        )

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(updatedPremises)
          .exchange()
          .expectStatus()
          .isForbidden
          .expectBody()
          .jsonPath("detail").isEqualTo("You are not authorized to access this endpoint")
      }
    }

    @Test
    fun `Update premises returns 404 when premises to update is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
        val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }
        val updatedPremises = Cas3UpdatePremises(
          reference = "reference",
          addressLine1 = "addressLine1",
          addressLine2 = "addressLine2",
          postcode = "postcode",
          town = "town",
          notes = "notes",
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = probationDeliveryUnit.id,
          localAuthorityAreaId = localAuthorityArea.id,
          characteristicIds = emptyList(),
          turnaroundWorkingDayCount = 2,
        )

        val id = UUID.randomUUID()

        webTestClient.put()
          .uri("/cas3/premises/$id")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(updatedPremises)
          .exchange()
          .expectStatus()
          .isNotFound()
          .expectBody()
          .jsonPath("detail").isEqualTo("No Premises with an ID of $id could be found")
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
        val (_, expectedPremisesSummaries) = getListPremises(user.probationRegion)

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

        bedEntityFactory.produceAndPersist {
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
          .jsonPath("$[0].bedspaceCount").isEqualTo(1)
          .jsonPath("$.length()").isEqualTo(1)
      }
    }

    @ParameterizedTest
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

        bedEntityFactory.produceAndPersist {
          withYieldedRoom { room }
        }

        bedEntityFactory.produceAndPersist {
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
          .jsonPath("$[0].bedspaceCount").isEqualTo(1)
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

        bedEntityFactory.produceAndPersist {
          withYieldedRoom { room }
        }

        bedEntityFactory.produceAndPersist {
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
          .jsonPath("$[0].bedspaceCount").isEqualTo(2)
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

      val premisesWithStartDateInFuture = getListPremisesByStatus(
        probationRegion = probationRegion,
        probationDeliveryUnit = probationDeliveryUnit,
        localAuthorityArea = localAuthorityArea,
        numberOfPremises = 2,
        startDate = LocalDate.now().plusDays(5),
        propertyStatus = PropertyStatus.active,
      ).map { premises ->
        val upcomingBedspaces = createBedspaces(premises, Cas3BedspaceStatus.upcoming)
        premisesSummary.add(createPremisesSummary(premises, upcomingBedspaces.size))
        premises
      }

      val allPremises = (onlinePremisesWithBedspaceWithoutEndDate + onlinePremisesWithBedspaceWithEndDate + archivedPremises + premisesWithStartDateInFuture)
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

        val expectedPremises = createCas3Premises(
          premisesToGet,
          user.probationRegion,
          probationDeliveryUnit,
          localAuthorityArea,
          Cas3PremisesStatus.online,
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

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3PremisesTest#getArchivedPremisesByIdCases")
    fun `Get Premises by ID returns OK with correct body when a premises is archived with future end date`(args: Pair<LocalDate, Cas3PremisesStatus>) {
      val (endDate, status) = args
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR), premisesStatus = PropertyStatus.archived, premisesEndDate = endDate) { user, jwt, premises ->
        premises.createdAt = OffsetDateTime.now().minusDays(120)
        premisesRepository.save(premises)

        val expectedPremises = createCas3Premises(
          premises,
          user.probationRegion,
          premises.probationDeliveryUnit!!,
          premises.localAuthorityArea!!,
          status,
          totalOnlineBedspaces = 0,
          totalUpcomingBedspaces = 0,
          totalArchivedBedspaces = 0,
        )

        val responseBody = webTestClient.get()
          .uri("/cas3/premises/${premises.id}")
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
    fun `Get Premises by ID returns OK with correct body when a premises start date is in the future`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStatus = PropertyStatus.active,
        premisesStartDate = LocalDate.now().plusDays(3),
      ) { user, jwt, premises ->
        val expectedPremises = createCas3Premises(
          premises,
          user.probationRegion,
          premises.probationDeliveryUnit!!,
          premises.localAuthorityArea!!,
          Cas3PremisesStatus.archived,
          premises.startDate,
          totalOnlineBedspaces = 0,
          totalUpcomingBedspaces = 0,
          totalArchivedBedspaces = 0,
        )

        val responseBody = webTestClient.get()
          .uri("/cas3/premises/${premises.id}")
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

    @SuppressWarnings("LongParameterList")
    private fun createCas3Premises(
      premises: TemporaryAccommodationPremisesEntity,
      probationRegion: ProbationRegionEntity,
      probationDeliveryUnit: ProbationDeliveryUnitEntity,
      localAuthorityArea: LocalAuthorityAreaEntity,
      status: Cas3PremisesStatus,
      scheduleUnarchiveDate: LocalDate? = null,
      totalOnlineBedspaces: Int,
      totalUpcomingBedspaces: Int,
      totalArchivedBedspaces: Int,
    ) = Cas3Premises(
      id = premises.id,
      reference = premises.name,
      addressLine1 = premises.addressLine1,
      addressLine2 = premises.addressLine2,
      postcode = premises.postcode,
      town = premises.town,
      probationRegion = ProbationRegion(probationRegion.id, probationRegion.name),
      probationDeliveryUnit = ProbationDeliveryUnit(probationDeliveryUnit.id, probationDeliveryUnit.name),
      localAuthorityArea = LocalAuthorityArea(
        localAuthorityArea.id,
        localAuthorityArea.identifier,
        localAuthorityArea.name,
      ),
      startDate = premises.createdAt.toLocalDate(),
      endDate = premises.endDate,
      scheduleUnarchiveDate = scheduleUnarchiveDate,
      status = status,
      characteristics = premises.characteristics.sortedBy { it.id }.map { characteristic ->
        Characteristic(
          id = characteristic.id,
          name = characteristic.name,
          propertyName = characteristic.propertyName,
          serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
          modelScope = Characteristic.ModelScope.forValue(characteristic.modelScope),
        )
      },
      notes = premises.notes,
      turnaroundWorkingDays = premises.turnaroundWorkingDays,
      totalOnlineBedspaces = totalOnlineBedspaces,
      totalUpcomingBedspaces = totalUpcomingBedspaces,
      totalArchivedBedspaces = totalArchivedBedspaces,
      archiveHistory = emptyList(),
    )
  }

  @Nested
  inner class GetPremisesBedspaces {
    @Test
    fun `Given a premises with bedspaces when get premises bedspaces then returns OK with correct bedspaces sorted`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val expectedBedspaces = mutableListOf<Cas3Bedspace>()

        // online bedspaces
        val bedspaceOne = createBedspaceInPremises(premises, startDate = LocalDate.now().minusMonths(6), endDate = null)
        bedspaceOne.createdAt = OffsetDateTime.now().minusMonths(7)
        bedRepository.saveAndFlush(bedspaceOne)
        expectedBedspaces.add(createCas3Bedspace(bedspaceOne, bedspaceOne.room, Cas3BedspaceStatus.online))

        val bedspaceTwo = createBedspaceInPremises(premises, startDate = LocalDate.now().minusMonths(5), endDate = LocalDate.now().plusDays(5))
        bedspaceTwo.createdAt = OffsetDateTime.now().minusMonths(5)
        bedRepository.saveAndFlush(bedspaceTwo)
        expectedBedspaces.add(createCas3Bedspace(bedspaceTwo, bedspaceTwo.room, Cas3BedspaceStatus.online))

        // upcoming bedspaces
        val bedspaceThree = createBedspaceInPremises(premises, startDate = LocalDate.now().plusDays(5), endDate = null)
        expectedBedspaces.add(createCas3Bedspace(bedspaceThree, bedspaceThree.room, Cas3BedspaceStatus.upcoming, bedspaceThree.startDate))

        // archived bedspaces
        val bedspaceFour = createBedspaceInPremises(
          premises,
          startDate = LocalDate.now().minusMonths(4),
          endDate = LocalDate.now().minusDays(1),
        )
        expectedBedspaces.add(createCas3Bedspace(bedspaceFour, bedspaceFour.room, Cas3BedspaceStatus.archived))

        val bedspaceFive = createBedspaceInPremises(
          premises,
          startDate = LocalDate.now().minusMonths(9),
          endDate = LocalDate.now().minusWeeks(1),
        )
        expectedBedspaces.add(createCas3Bedspace(bedspaceFive, bedspaceFive.room, Cas3BedspaceStatus.archived))

        val expectedCas3Bedspaces = Cas3Bedspaces(
          bedspaces = expectedBedspaces,
          totalOnlineBedspaces = 2,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 2,
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/premises/${premises.id}/bedspaces",
          expectedCas3Bedspaces,
        )
      }
    }

    @Test
    fun `Given a premises with bedspaces when get premises bedspaces then returns OK with correct bedspaces and archive history events in chronological order`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val expectedBedspaces = mutableListOf<Cas3Bedspace>()

        // online bedspaces
        val bedspaceOne = createBedspaceInPremises(premises, startDate = LocalDate.now().minusWeeks(1), endDate = null)
        expectedBedspaces.add(
          getExpectedBedspaceWithArchiveHistory(
            bedspaceOne,
            premises.id,
            user.id,
            Cas3BedspaceStatus.online,
            listOf(
              Cas3BedspaceStatus.archived to LocalDate.now().minusMonths(1),
              Cas3BedspaceStatus.online to LocalDate.now().minusWeeks(1),
            ),
          ),
        )

        val bedspaceTwo = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(5), endDate = LocalDate.now().plusDays(5))
        val archiveBedspaceInFiveDays = LocalDate.now().plusDays(5)
        createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, null, archiveBedspaceInFiveDays)
        expectedBedspaces.add(
          getExpectedBedspaceWithArchiveHistory(
            bedspaceTwo,
            premises.id,
            user.id,
            Cas3BedspaceStatus.online,
            listOf(
              Cas3BedspaceStatus.archived to LocalDate.now().minusMonths(2),
              Cas3BedspaceStatus.online to LocalDate.now().minusMonths(1),
            ),
          ),
        )

        // upcoming bedspaces
        val bedspaceThree = createBedspaceInPremises(premises, startDate = LocalDate.now().randomDateAfter(30), endDate = null)
        expectedBedspaces.add(createCas3Bedspace(bedspaceThree, bedspaceThree.room, Cas3BedspaceStatus.upcoming, bedspaceThree.startDate))

        // archived bedspaces
        val bedspaceFour = createBedspaceInPremises(premises, startDate = LocalDate.now().minusMonths(4), endDate = LocalDate.now().minusDays(1))
        expectedBedspaces.add(
          getExpectedBedspaceWithArchiveHistory(
            bedspaceFour,
            premises.id,
            user.id,
            Cas3BedspaceStatus.archived,
            listOf(
              Cas3BedspaceStatus.online to LocalDate.now().minusWeeks(2),
              Cas3BedspaceStatus.archived to LocalDate.now().minusDays(1),
            ),
          ),
        )

        val bedspaceFive = createBedspaceInPremises(premises, startDate = LocalDate.now().minusMonths(9), endDate = LocalDate.now())
        expectedBedspaces.add(
          getExpectedBedspaceWithArchiveHistory(
            bedspaceFive,
            premises.id,
            user.id,
            Cas3BedspaceStatus.archived,
            listOf(
              Cas3BedspaceStatus.archived to LocalDate.now(),
            ),
          ),
        )

        val expectedCas3Bedspaces = Cas3Bedspaces(
          bedspaces = expectedBedspaces,
          totalOnlineBedspaces = 2,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 2,
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/premises/${premises.id}/bedspaces",
          expectedCas3Bedspaces,
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

    private fun getExpectedBedspaceWithArchiveHistory(
      bedspace: BedEntity,
      premisesId: UUID,
      userId: UUID,
      status: Cas3BedspaceStatus,
      history: List<Pair<Cas3BedspaceStatus, LocalDate>>,
    ): Cas3Bedspace {
      history.forEach { (eventStatus, date) ->
        when (eventStatus) {
          Cas3BedspaceStatus.archived -> createBedspaceArchiveDomainEvent(bedspace.id, premisesId, userId, null, date)
          Cas3BedspaceStatus.online -> createBedspaceUnarchiveDomainEvent(
            bedspace.copy(endDate = date),
            premisesId,
            userId,
            date,
          )
          Cas3BedspaceStatus.upcoming -> null
        }
      }

      return createCas3Bedspace(
        bedspace,
        bedspace.room,
        status,
        archiveHistory = history.map { Cas3BedspaceArchiveAction(it.first, it.second) },
      )
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

        bedspace.createdAt = OffsetDateTime.now().minusDays(70)
        bedRepository.saveAndFlush(bedspace)

        val archiveBedspaceYesterday = LocalDate.now().minusDays(1)
        createBedspaceArchiveDomainEvent(bedspace.id, premises.id, user.id, null, archiveBedspaceYesterday)

        val archiveBedspace3DaysAgo = LocalDate.now().minusDays(3)
        createBedspaceArchiveDomainEvent(bedspace.id, premises.id, user.id, null, archiveBedspace3DaysAgo)

        createBedspaceArchiveDomainEvent(
          bedspace.id,
          premises.id,
          user.id,
          null,
          LocalDate.now().minusDays(10),
          OffsetDateTime.now().minusDays(13),
        )

        val archiveBedspaceDayAfterTomorrow = LocalDate.now().plusDays(2)
        createBedspaceArchiveDomainEvent(bedspace.id, premises.id, user.id, null, archiveBedspaceDayAfterTomorrow)

        val archivedBedspace = bedspace.copy(
          endDate = LocalDate.now().minusDays(7),
        )

        val unarchiveBedspaceToday = LocalDate.now()
        createBedspaceUnarchiveDomainEvent(archivedBedspace, premises.id, user.id, unarchiveBedspaceToday)

        val unarchiveBedspace4DaysAgo = LocalDate.now().minusDays(4)
        createBedspaceUnarchiveDomainEvent(archivedBedspace, premises.id, user.id, unarchiveBedspace4DaysAgo)

        createBedspaceUnarchiveDomainEvent(
          archivedBedspace,
          premises.id,
          user.id,
          LocalDate.now().minusDays(15),
          OffsetDateTime.now().minusDays(9),
        )

        val unarchiveBedspaceTomorrow = LocalDate.now().plusDays(1)
        createBedspaceUnarchiveDomainEvent(archivedBedspace, premises.id, user.id, unarchiveBedspaceTomorrow)

        val expectedBedspace = Cas3Bedspace(
          id = bedspace.id,
          reference = bedspace.room.name,
          startDate = LocalDate.now().minusDays(70),
          endDate = bedspace.endDate,
          status = Cas3BedspaceStatus.online,
          archiveHistory = listOf(
            Cas3BedspaceArchiveAction(
              status = Cas3BedspaceStatus.online,
              date = unarchiveBedspace4DaysAgo,
            ),
            Cas3BedspaceArchiveAction(
              status = Cas3BedspaceStatus.archived,
              date = archiveBedspace3DaysAgo,
            ),
            Cas3BedspaceArchiveAction(
              status = Cas3BedspaceStatus.archived,
              date = archiveBedspaceYesterday,
            ),
            Cas3BedspaceArchiveAction(
              status = Cas3BedspaceStatus.online,
              date = unarchiveBedspaceToday,
            ),
          ),
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
        val (_, bedspace) = createPremisesAndBedspace(user.probationRegion, emptyList())

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
        val (premises, _) = createPremisesAndBedspace(user.probationRegion, emptyList())

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
    fun `Create new bedspace in a scheduled to archive premises returns 201 Created with correct body and unarchive the premises`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStatus = PropertyStatus.archived,
        premisesEndDate = LocalDate.now().plusDays(3),
      ) { user, jwt, premises ->
        val bedspaceStartDate = LocalDate.now().minusDays(2)
        val characteristics = characteristicEntityFactory.produceAndPersistMultiple(5) {
          withModelScope("room")
          withServiceScope(ServiceName.temporaryAccommodation.value)
          withName(randomStringMultiCaseWithNumbers(10))
        }

        val characteristicIds = characteristics.map { it.id }

        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = bedspaceStartDate,
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
          .jsonPath("startDate").isEqualTo(LocalDate.now())
          .jsonPath("notes").isEqualTo(newBedspace.notes)
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
          .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })

        // verify premises is unarchived
        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.startDate).isEqualTo(bedspaceStartDate)
        assertThat(updatedPremises.endDate).isNull()

        val premisesUnarchiveDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesUnarchiveDomainEvents).isNotNull()
        assertThat(premisesUnarchiveDomainEvents.size).isEqualTo(1)
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
    fun `When create a new bedspace with start date before premises start date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = createPremises(user.probationRegion)

        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = premises.startDate.minusDays(3),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.startDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("startDateBeforePremisesStartDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(premises.startDate.toString())
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
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

  @Nested
  inner class ArchiveBedspace {
    @Test
    fun `When archive a bedspace returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val bedspaceOne = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)
        createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(180), endDate = null)
        // upcoming bedspace
        createBedspaceInPremises(premises, startDate = LocalDate.now().plusDays(4), endDate = null)
        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspaceOne.id)
          .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(1)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val bedspaceOne = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)
        createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(180), endDate = LocalDate.now().minusDays(2))

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspaceOne.id)
          .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(2)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
        assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)

        val archivedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(archivedPremises).isNotNull()
        assertThat(archivedPremises?.endDate).isEqualTo(archiveBedspace.endDate)
        assertThat(archivedPremises?.status).isEqualTo(PropertyStatus.archived)
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises with the latest bedspae end date when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val latestBedspaceArchiveDate = LocalDate.now().plusDays(35)
        val bedspaceOne = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)
        createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(180), endDate = latestBedspaceArchiveDate)

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspaceOne.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspaceOne.id)
          .jsonPath("endDate").isEqualTo(archiveBedspace.endDate)

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(2)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
        assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)

        val archivedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(archivedPremises).isNotNull()
        assertThat(archivedPremises?.endDate).isEqualTo(latestBedspaceArchiveDate)
        assertThat(archivedPremises?.status).isEqualTo(PropertyStatus.archived)
      }
    }

    @Test
    fun `When archive a bedspace for a Premises that not exist returns 404 Not Found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = createPremises(probationRegionEntityFactory.produceAndPersist())
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)
        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        val nonExistPremisesId = UUID.randomUUID()

        webTestClient.post()
          .uri("/cas3/premises/$nonExistPremisesId/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $nonExistPremisesId could be found")
      }
    }

    @Test
    fun `When archive a bedspace with end date before bedspace start date returns 404 Not Found`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR), premisesStartDate = LocalDate.now().minusDays(3)) { user, jwt, premises ->

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(2), endDate = null)

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().minusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateBeforeBedspaceStartDate")
      }
    }

    @Test
    fun `When archive a bedspace with a date that clashes with an earlier archive bedspace end date then returns 400 Bad Request`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val previousBedspaceArchiveDate = LocalDate.now().minusDays(3)

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(300), endDate = null)

        createBedspaceArchiveDomainEvent(bedspace.id, premises.id, user.id, null, previousBedspaceArchiveDate)

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().minusDays(3))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateOverlapPreviousBedspaceArchiveEndDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(previousBedspaceArchiveDate.toString())
      }
    }

    @Test
    fun `Can archive bedspace returns 400 when a bedspace have an active booking after the future archiving date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)

        val bookingDepartureDate = LocalDate.now().plusDays(10)
        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now().minusDays(20))
          withDepartureDate(bookingDepartureDate)
        }

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingBookings")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(bookingDepartureDate.plusDays(1).toString())
      }
    }

    @Test
    fun `When archive a bedspace with a void end date after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)

        val bedspaceArchivingDate = LocalDate.now().plusDays(10)
        cas3VoidBedspaceEntityFactory.produceAndPersist {
          withBed(bedspace)
          withPremises(premises)
          withStartDate(bedspaceArchivingDate.minusDays(2))
          withEndDate(bedspaceArchivingDate.plusDays(2))
          withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
        }

        val archiveBedspace = Cas3ArchiveBedspace(bedspaceArchivingDate)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingVoid")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(bedspaceArchivingDate.plusDays(3).toString())
      }
    }

    @Test
    fun `When archive a bedspace with a booking turnaround after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        clock.setNow(LocalDateTime.parse("2025-07-03T10:15:30"))

        govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now(clock).minusDays(360))
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(360), endDate = null)

        val bedspaceArchivingDate = LocalDate.now(clock).plusDays(10)
        val booking = bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bedspace)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(randomStringMultiCaseWithNumbers(8))
          withStatus(BookingStatus.provisional)
          withArrivalDate(LocalDate.now(clock).minusDays(20))
          withDepartureDate(bedspaceArchivingDate.minusDays(1))
        }

        cas3TurnaroundFactory.produceAndPersist {
          withBooking(booking)
          withWorkingDayCount(3)
        }

        val archiveBedspace = Cas3ArchiveBedspace(bedspaceArchivingDate)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingTurnaround")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspace.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(bedspaceArchivingDate.plusDays(4).toString())
      }
    }

    @Test
    fun `When archive a bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = createPremises(probationRegionEntityFactory.produceAndPersist())
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(360), endDate = null)
        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archiveBedspace)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  private fun createPremises(probationRegion: ProbationRegionEntity): TemporaryAccommodationPremisesEntity = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withYieldedProbationRegion { probationRegion }
  }

  @Nested
  inner class UnarchiveBedspace {
    @Test
    fun `Unarchive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(30), endDate = LocalDate.now().minusDays(1))

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(restartDate)
        assertThat(updatedBedspace.endDate).isNull()
      }
    }

    @Test
    fun `Unarchive bedspace when premises is archived returns 200 OK and unarchive premises and bedspace successfully`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withEndDate(LocalDate.now().minusDays(30))
          withStatus(PropertyStatus.archived)
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(180), endDate = LocalDate.now().minusDays(40))

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(restartDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(2)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
        assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_UNARCHIVED)
      }
    }

    @Test
    fun `Unarchive bedspace when premises is online returns 200 OK and unarchive bedspace successfully`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withEndDate(null)
          withStatus(PropertyStatus.active)
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(180), endDate = LocalDate.now().minusDays(40))

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was not updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(1)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is too far in the past`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(30), endDate = LocalDate.now().minusDays(10))

        val restartDate = LocalDate.now().minusDays(8) // More than 7 days in the past

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInThePast")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is too far in the future`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(30), endDate = LocalDate.now().minusDays(1))

        val restartDate = LocalDate.now().plusDays(8) // More than 7 days in the future

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInTheFuture")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is before last archive end date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val lastArchiveEndDate = LocalDate.now().minusDays(5)

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(30), endDate = lastArchiveEndDate)

        val restartDate = lastArchiveEndDate.minusDays(1) // Before the last archive end date

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("beforeLastBedspaceArchivedDate")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when bedspace does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val nonExistentBedspaceId = UUID.randomUUID()
        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when bedspace is not archived`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val onlineBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(10), endDate = null)

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${onlineBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceNotArchived")
      }
    }

    @Test
    fun `Unarchive bedspace returns 403 when user does not have permission to manage premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val archivedBedspace = createBedspaceInPremises(premises, startDate = LocalDate.now().minusDays(30), endDate = LocalDate.now().minusDays(1))

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class ArchivePremises {
    @Test
    fun `Given archive a premises when successfully passed all validations then returns 200 OK`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedEndDates = listOf(
            null,
            LocalDate.now().minusDays(2),
            null,
          ),
          bedspaceCount = 3,
        ) { premises, rooms, bedspaces ->
          val archivePremises = Cas3ArchivePremises(LocalDate.now())
          val bedspaceOne = bedspaces.first()
          val bedspaceTwo = bedspaces.drop(1).first()
          val bedspaceThree = bedspaces.drop(2).first()

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/archive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("id").isEqualTo(premises.id.toString())
            .jsonPath("status").isEqualTo("archived")

          val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
          assertThat(updatedPremises.status).isEqualTo(PropertyStatus.archived)
          assertThat(updatedPremises.endDate).isEqualTo(LocalDate.now())

          val updatedBedspaces = bedRepository.findByRoomPremisesId(updatedPremises.id)
          assertThat(updatedBedspaces).hasSize(3)
          assertThat(updatedBedspaces[0].id).isEqualTo(bedspaceTwo.id)
          assertThat(updatedBedspaces[0].endDate).isEqualTo(bedspaceTwo.endDate)
          assertThat(updatedBedspaces[1].id).isEqualTo(bedspaceOne.id)
          assertThat(updatedBedspaces[1].endDate).isEqualTo(LocalDate.now())
          assertThat(updatedBedspaces[2].id).isEqualTo(bedspaceThree.id)
          assertThat(updatedBedspaces[2].endDate).isEqualTo(LocalDate.now())
        }
      }
    }

    @Test
    fun `Given archive a premises without bedspaces when successfully passed all validations then returns 200 OK`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt, premises ->
        val archivePremises = Cas3ArchivePremises(LocalDate.now())

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.archived)
        assertThat(updatedPremises.endDate).isEqualTo(LocalDate.now())
      }
    }

    @Test
    fun `Given archive a premises when archive date is more than 7 days in the past then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
        ) { premises, rooms, bedspaces ->

          val archivePremises = Cas3ArchivePremises(LocalDate.now().minusDays(8))

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/archive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidEndDateInThePast")
        }
      }
    }

    @Test
    fun `Given archive a premises when archive date is more than 3 months in then future then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
        ) { premises, rooms, bedspaces ->

          val archivePremises = Cas3ArchivePremises(LocalDate.now().plusMonths(3).plusDays(1))

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/archive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidEndDateInTheFuture")
        }
      }
    }

    @Test
    fun `Given archive a premises when archive date is before premises start date then returns 400 Bad Request`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(3),
      ) { user, jwt, premises ->

        val archivePremises = Cas3ArchivePremises(premises.startDate.minusDays(2))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateBeforePremisesStartDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(premises.startDate.toString())
      }
    }

    @Test
    fun `Given archive a premises when archive date clashes with an earlier archive premises end date then returns 400 Bad Request`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val previousPremisesArchiveDate = LocalDate.now().minusDays(3)

        createPremisesArchiveDomainEvent(premises, user, previousPremisesArchiveDate)

        val archivePremises = Cas3ArchivePremises(previousPremisesArchiveDate.minusDays(3))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/archive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(archivePremises)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("endDateOverlapPreviousPremisesArchiveEndDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(previousPremisesArchiveDate.toString())
      }
    }

    @Test
    fun `Given archive a premises when there is upcoming bedspace then returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedStartDates = listOf(LocalDate.now().minusDays(100), LocalDate.now().plusDays(5)),
        ) { premises, rooms, bedspaces ->

          val upcomingBedspace = bedspaces.drop(1).first()
          val archivePremises = Cas3ArchivePremises(LocalDate.now().plusDays(1))

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/archive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingUpcomingBedspace")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(upcomingBedspace.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(upcomingBedspace.startDate?.plusDays(1).toString())
        }
      }
    }

    @Test
    fun `Given archive a premises when bedspaces have active booking and void after the premises archive date then returns 400 Bad Request with correct details`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedspaceCount = 3,
        ) { premises, rooms, bedspaces ->
          val premisesArchiveDate = LocalDate.now().plusDays(5)

          val bedspaceOne = bedspaces.first()
          val bookingDepartureDate = premisesArchiveDate.plusDays(10)
          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bedspaceOne)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(randomStringMultiCaseWithNumbers(8))
            withStatus(BookingStatus.provisional)
            withArrivalDate(premisesArchiveDate.minusDays(2))
            withDepartureDate(bookingDepartureDate)
          }

          val bedspaceTwo = bedspaces.drop(1).first()
          val voidEndDate = premisesArchiveDate.plusDays(5)
          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspaceTwo)
            withPremises(premises)
            withStartDate(premisesArchiveDate.minusDays(2))
            withEndDate(voidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          val bedspaceThree = bedspaces.drop(2).first()
          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bedspaceThree)
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn(randomStringMultiCaseWithNumbers(8))
            withStatus(BookingStatus.arrived)
            withArrivalDate(premisesArchiveDate.minusDays(35))
            withDepartureDate(premisesArchiveDate.plusDays(3))
          }

          val archivePremises = Cas3ArchivePremises(premisesArchiveDate)

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/archive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(archivePremises)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.title").isEqualTo("Bad Request")
            .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.endDate")
            .jsonPath("$.invalid-params[0].errorType").isEqualTo("existingBookings")
            .jsonPath("$.invalid-params[0].entityId").isEqualTo(bedspaceOne.id.toString())
            .jsonPath("$.invalid-params[0].value").isEqualTo(bookingDepartureDate.plusDays(1).toString())
        }
      }
    }
  }

  @Nested
  inner class UnarchivePremises {
    @Test
    fun `Unarchive premises returns 200 OK when successful`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 3,
        bedStartDates = listOf(
          LocalDate.now().minusDays(30),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(14),
        ),
        bedEndDates = listOf(
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(1),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.startDate).isEqualTo(restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val updatedBedspaces = bedRepository.findAll()
        assertThat(updatedBedspaces).hasSize(3)
        updatedBedspaces.forEach { bedspace ->
          assertThat(bedspace.startDate).isEqualTo(restartDate)
          assertThat(bedspace.endDate).isNull()
        }
      }
    }

    @Test
    fun `Given unarchive premises with duplicated bedspace reference returns 200 OK and unarchive the lastest created bedspace with the same reference`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(180),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 3,
        bedStartDates = listOf(
          LocalDate.now().minusDays(30),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(14),
        ),
        bedEndDates = listOf(
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(1),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val restartDate = LocalDate.now().plusDays(1)

        val originalRoom = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withName(randomStringMultiCaseWithNumbers(10))
          withNotes(randomStringLowerCase(100))
        }

        val duplicatedRoom = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withName(originalRoom.name)
          withNotes(randomStringLowerCase(100))
        }

        val originalBedspace = bedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().minusDays(180))
          withEndDate(LocalDate.now().minusDays(170))
          withRoom(originalRoom)
          withCreatedAt { OffsetDateTime.now().minusDays(180) }
        }

        val duplicatedBedspace = bedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().minusDays(160))
          withEndDate(LocalDate.now().minusDays(1))
          withRoom(duplicatedRoom)
          withCreatedAt { OffsetDateTime.now().minusDays(160) }
        }

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.startDate).isEqualTo(restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val allBedspaces = bedRepository.findAll()
        val updateBedspace = allBedspaces.filter { it.startDate == restartDate }
        assertThat(updateBedspace).hasSize(4)
        assertThat(updateBedspace.map { it.id }).contains(duplicatedBedspace.id)
        assertThat(updateBedspace.map { it.id }).doesNotContain(originalBedspace.id)
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is too far in the past`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withProbationDeliveryUnit(pdu)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(LocalDate.now().minusDays(10))
          withStatus(PropertyStatus.archived)
        }

        val restartDate = LocalDate.now().minusDays(8)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInThePast")
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is too far in the future`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
        }

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withProbationDeliveryUnit(pdu)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(LocalDate.now().minusDays(1))
          withStatus(PropertyStatus.archived)
        }

        val restartDate = LocalDate.now().plusDays(8)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("invalidRestartDateInTheFuture")
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is before last archive end date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(userEntity.probationRegion)
        }

        val lastArchiveEndDate = LocalDate.now().minusDays(5)
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withProbationDeliveryUnit(pdu)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(lastArchiveEndDate)
          withStatus(PropertyStatus.archived)
        }

        val restartDate = lastArchiveEndDate.minusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.restartDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("beforeLastPremisesArchivedDate")
      }
    }

    @Test
    fun `Unarchive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()
        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/$nonExistentPremisesId/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.title").isEqualTo("Not Found")
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $nonExistentPremisesId could be found")
      }
    }

    @Test
    fun `Unarchive premises returns 400 when premises is not archived`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
          withStatus(PropertyStatus.active)
        }

        val restartDate = LocalDate.now().plusDays(1)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            mapOf("restartDate" to restartDate.toString()),
          )
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesNotArchived")
      }
    }

    @Test
    fun `Unarchive premises returns 403 when user does not have permission to manage premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAProbationRegion { otherRegion ->
          val pdu = probationDeliveryUnitFactory.produceAndPersist {
            withProbationRegion(otherRegion)
          }

          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
            withYieldedProbationRegion { otherRegion }
            withProbationDeliveryUnit(pdu)
            withStartDate(LocalDate.now().minusDays(30))
            withEndDate(LocalDate.now().minusDays(1))
            withStatus(PropertyStatus.archived)
          }

          val restartDate = LocalDate.now().plusDays(1)

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/unarchive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              mapOf("restartDate" to restartDate.toString()),
            )
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class CancelScheduledArchiveBedspace {
    @Test
    fun `Cancel scheduled archive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        clock.setNow(LocalDateTime.parse("2025-08-25T10:15:30"))

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { user.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val scheduledArchiveBedspaceDate = LocalDate.now().plusDays(1)
        val previousBedspaceEndDate = LocalDate.now().minusDays(10)
        val bedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(scheduledArchiveBedspaceDate)
        }

        val bedspaceArchiveDomainEvent = createBedspaceArchiveDomainEvent(
          bedspace.id,
          premises.id,
          user.id,
          previousBedspaceEndDate,
          scheduledArchiveBedspaceDate,
        )

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspace.id)
          .jsonPath("endDate").isEqualTo(previousBedspaceEndDate)

        assertThatBedspaceArchiveCancelled(bedspace.id, bedspaceArchiveDomainEvent.id, previousBedspaceEndDate)
      }
    }

    @Test
    fun `When cancel scheduled archive bedspace in a premsies that is scheduled to be archived returns Success and cancels the scheduled archiving of the premises and all associated bedspaces`() {
      val archivedPremisesDate = LocalDate.now().plusWeeks(1)
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = archivedPremisesDate,
        premisesStatus = PropertyStatus.active,
        bedspaceCount = 3,
        bedEndDates = listOf(
          archivedPremisesDate,
          archivedPremisesDate,
          archivedPremisesDate,
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        clock.setNow(LocalDateTime.parse("2025-08-27T15:21:34"))

        val bedspaceOne = bedspaces.first()
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceThree = bedspaces.drop(2).first()

        val previousBedspaceOneEndDate = LocalDate.now().minusDays(10)

        val archivePremisesDomainEvent = createPremisesArchiveDomainEvent(premises, user, archivedPremisesDate)
        createBedspaceArchiveDomainEvent(
          bedspaceOne.id,
          premises.id,
          user.id,
          previousBedspaceOneEndDate,
          archivedPremisesDate,
        )
        val archiveBedspaceTwoDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, null, archivedPremisesDate)
        val archiveBedspaceThreeDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, archivedPremisesDate)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspaceOne.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(bedspaceOne.id)
          .jsonPath("endDate").isEqualTo(previousBedspaceOneEndDate)

        // check that premises was updated correctly
        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.endDate).isNull()

        val updatedArchivePremisesDomainEvent = domainEventRepository.findByIdOrNull(archivePremisesDomainEvent.id)
        assertThat(updatedArchivePremisesDomainEvent).isNotNull()
        assertThat(updatedArchivePremisesDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // check that bedspaces were updated correctly
        assertThatBedspaceArchiveCancelled(bedspaceTwo.id, archiveBedspaceTwoDomainEvent.id, null)
        assertThatBedspaceArchiveCancelled(bedspaceThree.id, archiveBedspaceThreeDomainEvent.id, null)
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val scheduledToArchivedBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(LocalDate.now().plusDays(1)) // Archived tomorrow
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledToArchivedBedspace.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val nonExistentBedspaceId = UUID.randomUUID()

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("doesNotExist")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace is not archived`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val onlineBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(10))
          withEndDate(null) // Not archived
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${onlineBedspace.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceNotScheduledToArchive")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 400 when bedspace is already archived`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { userEntity.probationRegion }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val onlineBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(10))
          withEndDate(LocalDate.now().minusDays(1)) // Already archived
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${onlineBedspace.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 404 when the premises is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->

        webTestClient.put()
          .uri("/cas3/premises/${UUID.randomUUID()}/bedspaces/${UUID.randomUUID()}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val archivedBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(LocalDate.now().minusDays(1))
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    private fun assertThatBedspaceArchiveCancelled(bedspaceId: UUID, archiveBedspaceDomainEventId: UUID, previousEndDate: LocalDate?) {
      val updatedBedspace = bedRepository.findById(bedspaceId).get()
      assertThat(updatedBedspace.endDate).isEqualTo(previousEndDate)

      val updatedBedspaceArchiveDomainEvent =
        domainEventRepository.findByIdOrNull(archiveBedspaceDomainEventId)
      assertThat(updatedBedspaceArchiveDomainEvent).isNotNull()
      assertThat(updatedBedspaceArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
    }
  }

  @Nested
  inner class CancelScheduledUnarchiveBedspace {
    @Test
    fun `Cancel scheduled unarchive bedspace returns 200 OK when successful`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { userEntity, jwt, premises ->
        clock.setNow(LocalDateTime.parse("2025-08-27T15:21:34"))

        val originalStartDate = LocalDate.now(clock).minusDays(10)
        val originalEndDate = LocalDate.now(clock).minusDays(3)

        val createdAt = OffsetDateTime.now().minusDays(120)
        val scheduledBedspaceToUnarchived = createBedspaceInPremises(premises, originalStartDate, originalEndDate)
        scheduledBedspaceToUnarchived.createdAt = createdAt
        bedRepository.saveAndFlush(scheduledBedspaceToUnarchived)

        createBedspaceUnarchiveDomainEvent(scheduledBedspaceToUnarchived, premises.id, userEntity.id, LocalDate.now(clock).minusDays(30))
        val lastBedspaceUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(scheduledBedspaceToUnarchived, premises.id, userEntity.id, LocalDate.now(clock).plusDays(10))

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledBedspaceToUnarchived.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(scheduledBedspaceToUnarchived.id)
          .jsonPath("startDate").isEqualTo(createdAt.toLocalDate())
          .jsonPath("endDate").isEqualTo(originalEndDate)

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(scheduledBedspaceToUnarchived.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(originalStartDate)
        assertThat(updatedBedspace.endDate).isEqualTo(originalEndDate)

        assertThatBedspaceUnarchiveCancelled(scheduledBedspaceToUnarchived.id, lastBedspaceUnarchiveDomainEvent.id, originalStartDate)
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 400 when it has a field validation error (startDate is today)`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt, premises ->
        val startDate = LocalDate.now()

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val scheduledToUnarchivedBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(startDate)
          withEndDate(null)
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.bedspaceId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("bedspaceAlreadyOnline")

        // Verify the bedspace was not updated
        val originalBedspace = bedRepository.findById(scheduledToUnarchivedBedspace.id).get()
        assertThat(originalBedspace.startDate).isEqualTo(startDate)
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_REFERRER)) { _, jwt, premises ->
        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val scheduledToUnarchivedBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(null)
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 404 when the bedspace is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->

        webTestClient.put()
          .uri("/cas3/premises/${UUID.randomUUID()}/bedspaces/${UUID.randomUUID()}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }
        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val scheduledToUnarchivedBedspace = bedEntityFactory.produceAndPersist {
          withRoom(room)
          withStartDate(LocalDate.now().minusDays(30))
          withEndDate(null)
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    private fun assertThatBedspaceUnarchiveCancelled(bedspaceId: UUID, unarchiveBedspaceDomainEventId: UUID, previousStartDate: LocalDate) {
      val updatedBedspace = bedRepository.findById(bedspaceId).get()
      assertThat(updatedBedspace.startDate).isEqualTo(previousStartDate)

      val updatedBedspaceUnarchiveDomainEvent =
        domainEventRepository.findByIdOrNull(unarchiveBedspaceDomainEventId)
      assertThat(updatedBedspaceUnarchiveDomainEvent).isNotNull()
      assertThat(updatedBedspaceUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
    }
  }

  @Nested
  inner class CancelScheduledUnarchivePremises {
    @Test
    fun `Cancel scheduled unarchive premises returns 200 OK when successful`() {
      val previousStartDate = LocalDate.now().minusDays(60)
      val previousEndDate = previousStartDate.plusDays(30)
      val newStartDate = LocalDate.now().plusDays(5)
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = previousStartDate,
        premisesEndDate = previousEndDate,
        premisesStatus = PropertyStatus.archived,
      ) { userEntity, jwt, premises ->
        clock.setNow(LocalDateTime.parse("2025-07-16T21:33:04"))

        premises.createdAt = OffsetDateTime.now().minusDays(60)
        premisesRepository.saveAndFlush(premises)

        val bedspace = createBedspaceInPremises(premises, previousStartDate, previousEndDate)

        // previous unarchive domain events
        createPremisesUnarchiveDomainEvent(
          premises,
          userEntity,
          previousEndDate.minusDays(180),
          previousEndDate.plusDays(15),
          previousStartDate.minusDays(30),
        )

        val lastPremisesUnarchiveDomainEvent = createPremisesUnarchiveDomainEvent(
          premises,
          userEntity,
          previousStartDate,
          newStartDate,
          previousEndDate,
        )

        createBedspaceUnarchiveDomainEvent(
          bedspace,
          premises.id,
          userEntity.id,
          previousStartDate.minusDays(40),
        )

        val lastBedspaceUnarchiveDomainEvent = createBedspaceUnarchiveDomainEvent(
          bedspace,
          premises.id,
          userEntity.id,
          newStartDate,
        )

        premises.startDate = newStartDate
        premises.endDate = null
        premises.status = PropertyStatus.active
        temporaryAccommodationPremisesRepository.save(premises)

        val updatedBedspace = bedspace.copy(
          startDate = newStartDate,
          endDate = null,
        )
        bedRepository.save(updatedBedspace)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("startDate").isEqualTo(previousStartDate)
          .jsonPath("endDate").isEqualTo(previousEndDate)

        // Verify that premise was updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.archived)
        assertThat(updatedPremises.startDate).isEqualTo(premises.createdAt?.toLocalDate())
        assertThat(updatedPremises.endDate).isEqualTo(previousEndDate)

        val updatedPremisesUnarchiveDomainEvent = domainEventRepository.findByIdOrNull(lastPremisesUnarchiveDomainEvent.id)
        assertThat(updatedPremisesUnarchiveDomainEvent).isNotNull()
        assertThat(updatedPremisesUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // Verify that bedspace was updated
        val updatedBed = bedRepository.findById(bedspace.id).get()
        assertThat(updatedBed.startDate).isEqualTo(previousStartDate)
        assertThat(updatedBed.endDate).isEqualTo(previousEndDate)

        val updatedBedspaceUnarchiveDomainEvent = domainEventRepository.findByIdOrNull(lastBedspaceUnarchiveDomainEvent.id)
        assertThat(updatedBedspaceUnarchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
      }
    }

    @Test
    fun `Cancel scheduled unarchive premises returns 404 when premises is not found`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val id = UUID.randomUUID()

        webTestClient.put()
          .uri("/cas3/premises/$id/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("detail").isEqualTo("No Premises with an ID of $id could be found")
      }
    }

    @Test
    fun `Cancel unarchive premises returns 403 when user is not authorized`() {
      givenAUser { _, jwt ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
        }

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-unarchive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class CancelScheduledArchivePremises {
    @Test
    fun `Cancel scheduled archive premises returns 200 OK when successful`() {
      clock.setNow(LocalDateTime.parse("2025-07-16T21:33:04"))

      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now().plusDays(10),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 3,
        bedEndDates = listOf(
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->

        val premisesArchiveDate = premises.endDate!!
        val premisesArchiveDomainEvent = createPremisesArchiveDomainEvent(premises, user, premisesArchiveDate)

        val bedspaceOne = bedspaces.first()
        val bedspaceOneArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceOne.id, premises.id, user.id, null, premisesArchiveDate)
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceTwoCurrentEndDate = bedspaceTwo.endDate ?: premisesArchiveDate.plusDays(12)
        val bedspaceTwoArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, bedspaceTwoCurrentEndDate, premisesArchiveDate)
        val bedspaceThree = bedspaces.drop(2).first()
        val bedspaceThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, premisesArchiveDate)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id)
          .jsonPath("endDate").doesNotExist()

        // Verify that premise was updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.endDate).isNull()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)

        val updatedPremisesArchiveDomainEvent = domainEventRepository.findByIdOrNull(premisesArchiveDomainEvent.id)
        assertThat(updatedPremisesArchiveDomainEvent).isNotNull()
        assertThat(updatedPremisesArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // Verify that bedspace were updated
        val updatedBedspaces = bedRepository.findByRoomPremisesId(premises.id)
        updatedBedspaces.containsAll(listOf(bedspaceOne, bedspaceTwo, bedspaceThree))
        assertThat(updatedBedspaces).hasSize(3)
        assertThat(updatedBedspaces.first { it.id == bedspaceOne.id }.endDate).isNull()
        assertThat(updatedBedspaces.first { it.id == bedspaceTwo.id }.endDate).isEqualTo(bedspaceTwoCurrentEndDate)
        assertThat(updatedBedspaces.first { it.id == bedspaceThree.id }.endDate).isNull()

        val updatedBedspaceOneArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceOneArchiveDomainEvent.id)
        assertThat(updatedBedspaceOneArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceOneArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        val updatedBedspaceTwoArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceTwoArchiveDomainEvent.id)
        assertThat(updatedBedspaceTwoArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceTwoArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        val updatedBedspaceThreeArchiveDomainEvent = domainEventRepository.findByIdOrNull(bedspaceThreeArchiveDomainEvent.id)
        assertThat(updatedBedspaceThreeArchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceThreeArchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_REFERRER),
      ) { _, jwt, premises ->
        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremises = UUID.randomUUID().toString()

        webTestClient.put()
          .uri("/cas3/premises/$nonExistentPremises/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.title").isEqualTo("Not Found")
          .jsonPath("$.status").isEqualTo(404)
          .jsonPath("$.detail").isEqualTo("No Premises with an ID of $nonExistentPremises could be found")
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 400 (premisesNotScheduledToArchive) when premise is not archived`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { _, jwt, premises ->
        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesNotScheduledToArchive")
      }
    }

    @Test
    fun `Cancel scheduled archive premise returns 400 when premise already archived today`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now(),
        premisesStatus = PropertyStatus.archived,
      ) { _, jwt, premises ->
        givenATemporaryAccommodationRooms(premises = premises)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive premise returns 400 when premise has already been archived in the past`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now().minusDays(1),
      ) { _, jwt, premises ->
        givenATemporaryAccommodationRooms(premises = premises)

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/cancel-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("\$.premisesId")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("premisesAlreadyArchived")
      }
    }

    @Test
    fun `Cancel scheduled archive premises returns 403 when user does not have permission to manage premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenATemporaryAccommodationPremises(region = givenAProbationRegion()) { premises ->
          webTestClient.put()
            .uri("/cas3/premises/${premises.id}/cancel-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class GetPremisesBedspaceTotals {
    @Test
    fun `Get premises bedspace totals returns 200 with correct totals`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(region = userEntity.probationRegion) { premises ->

          val rooms = roomEntityFactory.produceAndPersistMultiple(10) {
            withPremises(premises)
          }

          // Online
          rooms.take(3).forEach { room ->
            bedEntityFactory.produceAndPersist {
              withRoom(room)
              withStartDate(LocalDate.now().minusDays(10))
              withEndDate(null)
            }
          }

          // Upcoming
          rooms.drop(3).take(4).forEach { room ->
            bedEntityFactory.produceAndPersist {
              withRoom(room)
              withStartDate(LocalDate.now().plusDays(5))
              withEndDate(null)
            }
          }

          // Archived
          rooms.drop(7).forEach { room ->
            bedEntityFactory.produceAndPersist {
              withRoom(room)
              withStartDate(LocalDate.now().minusDays(30))
              withEndDate(LocalDate.now().minusDays(5))
            }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspace-totals")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(premises.id.toString())
            .jsonPath("$.status").isEqualTo("online")
            .jsonPath("$.premisesEndDate").isEqualTo(null)
            .jsonPath("$.totalOnlineBedspaces").isEqualTo(3)
            .jsonPath("$.totalUpcomingBedspaces").isEqualTo(4)
            .jsonPath("$.totalArchivedBedspaces").isEqualTo(3)
        }
      }
    }

    @Test
    fun `Get premises bedspace totals returns 200 with zero totals when premises has no bedspaces`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(region = userEntity.probationRegion) { premises ->
          roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspace-totals")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(premises.id.toString())
            .jsonPath("$.status").isEqualTo("online")
            .jsonPath("$.premisesEndDate").isEqualTo(null)
            .jsonPath("$.totalOnlineBedspaces").isEqualTo(0)
            .jsonPath("$.totalUpcomingBedspaces").isEqualTo(0)
            .jsonPath("$.totalArchivedBedspaces").isEqualTo(0)
        }
      }
    }

    @Test
    fun `Get premises bedspace totals returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()

        webTestClient.get()
          .uri("/cas3/premises/$nonExistentPremisesId/bedspace-totals")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Get premises bedspace totals returns 403 when user does not have permission to view premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises { premises ->
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspace-totals")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Get premises bedspace totals returns 200 with archived status for archived premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(
          region = userEntity.probationRegion,
          endDate = LocalDate.now().minusDays(1),
          status = PropertyStatus.archived,
        ) { premises ->

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspace-totals")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(premises.id.toString())
            .jsonPath("$.status").isEqualTo("archived")
            .jsonPath("$.premisesEndDate").isEqualTo(premises.endDate.toString())
            .jsonPath("$.totalOnlineBedspaces").isEqualTo(0)
            .jsonPath("$.totalUpcomingBedspaces").isEqualTo(0)
            .jsonPath("$.totalArchivedBedspaces").isEqualTo(0)
        }
      }
    }
  }

  @Nested
  inner class ArchiveHistory {
    @Test
    fun `Get premises by ID returns 200 with empty archive history for premises with no archive events`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(region = userEntity.probationRegion) { premises ->
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(premises.id.toString())
            .jsonPath("$.archiveHistory").isArray
            .jsonPath("$.archiveHistory").isEmpty
        }
      }
    }

    @Test
    fun `Get premises by ID returns 200 with multiple archive history events in chronological order`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(region = userEntity.probationRegion) { premises ->
          val firstArchiveDate = LocalDate.now().minusDays(5)
          val firstUnarchiveDate = LocalDate.now().minusDays(4)
          val secondArchiveDate = LocalDate.now().minusDays(3)
          val secondUnarchiveDate = LocalDate.now().minusDays(2)

          createPremisesArchiveDomainEvent(premises, userEntity, firstArchiveDate)
          createPremisesUnarchiveDomainEvent(
            premises,
            userEntity,
            LocalDate.now().minusDays(20),
            firstUnarchiveDate.plusDays(7),
            firstArchiveDate,
            OffsetDateTime.now().minusDays(5),
          )
          createPremisesUnarchiveDomainEvent(premises, userEntity, currentStartDate = LocalDate.now(), firstUnarchiveDate, firstArchiveDate)
          createPremisesArchiveDomainEvent(premises, userEntity, secondArchiveDate.minusDays(3), OffsetDateTime.now().minusDays(6))
          createPremisesArchiveDomainEvent(premises, userEntity, secondArchiveDate)
          createPremisesUnarchiveDomainEvent(premises, userEntity, LocalDate.now(), secondUnarchiveDate, secondArchiveDate)

          // Get premises and verify archive history is in chronological order
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(premises.id.toString())
            .jsonPath("$.archiveHistory").isArray
            .jsonPath("$.archiveHistory.length()").isEqualTo(4)
            .jsonPath("$.archiveHistory[0].status").isEqualTo(Cas3PremisesStatus.archived.name)
            .jsonPath("$.archiveHistory[0].date").isEqualTo(firstArchiveDate.toString())
            .jsonPath("$.archiveHistory[1].status").isEqualTo(Cas3PremisesStatus.online.name)
            .jsonPath("$.archiveHistory[1].date").isEqualTo(firstUnarchiveDate.toString())
            .jsonPath("$.archiveHistory[2].status").isEqualTo(Cas3PremisesStatus.archived.name)
            .jsonPath("$.archiveHistory[2].date").isEqualTo(secondArchiveDate.toString())
            .jsonPath("$.archiveHistory[3].status").isEqualTo(Cas3PremisesStatus.online.name)
            .jsonPath("$.archiveHistory[3].date").isEqualTo(secondUnarchiveDate.toString())
        }
      }
    }
  }

  @Nested
  inner class CanArchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-26T00:00:00Z"))
    }

    @Test
    fun `Can archive premises returns 200 when no blocking bookings or voids exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 2,
        ) { premises, rooms, beds ->
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings with provisional status have departure dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withStatus(BookingStatus.provisional)
            withArrivalDate(LocalDate.now(clock).minusDays(1))
            withDepartureDate(futureDepartureDate)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings with arrived status have departure dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withStatus(BookingStatus.arrived)
            withArrivalDate(LocalDate.now(clock).minusDays(1))
            withDepartureDate(futureDepartureDate)
          }

          arrivalEntityFactory.produceAndPersist { withBooking(booking) }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings with confirmed status have departure dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(5)

          val booking = bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premises)
            withBed(bed)
            withCrn("CRN123")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.confirmed)
          }

          cas3ConfirmationEntityFactory.produceAndPersist {
            withBooking(booking)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when voids have end dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureEndDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureEndDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when there are booking and void after 3 months will return the bedspace with the latest blocking date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(4)
          val latestBlockingDate = futureVoidEndDate.plusWeeks(1)

          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withCrn("CRN123")
            withYieldedPremises { premises }
            withYieldedBed { bed }
            withStatus(BookingStatus.provisional)
            withArrivalDate(LocalDate.now(clock).minusDays(1))
            withDepartureDate(latestBlockingDate)
          }

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(latestBlockingDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings have departure dates within 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val nearFutureDepartureDate = LocalDate.now(clock).plusMonths(2) // Within 3 months

          bookingEntityFactory.produceAndPersist {
            withServiceName(ServiceName.temporaryAccommodation)
            withPremises(premises)
            withBed(bed)
            withCrn("CRN123")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(nearFutureDepartureDate)
            withStatus(BookingStatus.provisional)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when voids have end dates within 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val nearFutureEndDate = LocalDate.now(clock).plusMonths(2)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(nearFutureEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 with multiple affected bedspaces when multiple bedspaces have blocking bookings or voids`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 2,
        ) { premises, rooms, beds ->
          val bed1 = beds[0]
          val bed2 = beds[1]
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(5)

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed1)
            withCrn("CRN111")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.provisional)
          }

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed2)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(2)
            .jsonPath("$.items[0].entityId").isEqualTo(bed1.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed1.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
            .jsonPath("$.items[1].entityId").isEqualTo(bed2.id.toString())
            .jsonPath("$.items[1].entityReference").isEqualTo(bed2.room.name)
            .jsonPath("$.items[1].date").isEqualTo(futureVoidEndDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when bookings are cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN000")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.provisional)
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()

        webTestClient.get()
          .uri("/cas3/premises/$nonExistentPremisesId/can-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Can archive premises returns 403 when user does not have permission to view premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises { premises ->
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 with edge case exactly 3 months - 1 day from today`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val exactlyThreeMonthsDate = LocalDate.now(clock).plusMonths(3).minusDays(1)

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN333")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(exactlyThreeMonthsDate)
            withStatus(BookingStatus.provisional)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items").isEmpty
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 with edge case, turnaround time exceeds 3 months limit`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->

          val bed = beds.first()
          val justUnder3Months = LocalDate.now(clock).plusMonths(3).minusDays(2)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN333")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(justUnder3Months)
            withStatus(BookingStatus.provisional)
          }

          cas3TurnaroundFactory.produceAndPersist {
            withWorkingDayCount(3)
            withBooking(booking)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.items[0].date").isEqualTo(justUnder3Months.plusDays(3))
        }
      }
    }
  }

  @Nested
  inner class CanArchiveBedspaceInFuture {

    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-26T00:00:00Z"))
    }

    @Test
    fun `Can archive bedspace returns 200 when there are no bookings or voids after 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty()
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when a provisional booking departure date is more than 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bedspace = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bedspace)
            withCrn("CRN123")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.provisional)
          }
          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bedspace.room.name)
            .jsonPath("$.date").isEqualTo(futureDepartureDate.toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when an arrived booking departure date is more than 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(5)

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN456")
            withArrivalDate(LocalDate.now(clock).minusDays(5))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.arrived)
          }

          arrivalEntityFactory.produceAndPersist { withBooking(booking) }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.date").isEqualTo(futureDepartureDate.toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when a confirmed booking departure date is more than 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(6)

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN789")
            withArrivalDate(LocalDate.now(clock).plusDays(10))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.confirmed)
          }

          cas3ConfirmationEntityFactory.produceAndPersist {
            withBooking(booking)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.date").isEqualTo(futureDepartureDate.toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 when a booking is cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN000")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(futureDepartureDate)
            withStatus(BookingStatus.provisional)
          }

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty()
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when a void end date is more than 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bed)
            withStartDate(LocalDate.now(clock).plusDays(10))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.date").isEqualTo(futureVoidEndDate.toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when booking turnaround date is more than 3 months from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val justUnder3Months = weekdayBefore(LocalDate.now(clock).plusMonths(3))

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN333")
            withArrivalDate(weekdayAfter(LocalDate.now(clock).plusDays(1)))
            withDepartureDate(justUnder3Months)
            withStatus(BookingStatus.provisional)
          }

          cas3TurnaroundFactory.produceAndPersist {
            withWorkingDayCount(3)
            withBooking(booking)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bed.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bed.room.name)
            .jsonPath("$.date").isEqualTo(weekdayAfter(justUnder3Months.plusDays(3)).toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 when booking departure date is 3 months minus 1 day from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, beds ->
          val bed = beds.first()
          val exactlyThreeMonthsMinusOneDay = LocalDate.now(clock).plusMonths(3).minusDays(1)

          bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withCrn("CRN333")
            withArrivalDate(LocalDate.now(clock).plusDays(1))
            withDepartureDate(exactlyThreeMonthsMinusOneDay)
            withStatus(BookingStatus.provisional)
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bed.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty()
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 404 when premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val nonExistentPremisesId = UUID.randomUUID()
        val nonExistentBedspaceId = UUID.randomUUID()

        webTestClient.get()
          .uri("/cas3/premises/$nonExistentPremisesId/bedspaces/$nonExistentBedspaceId/can-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Can archive bedspace returns 404 when bedspace does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremises(region = userEntity.probationRegion) { premises ->
          val nonExistentBedspaceId = UUID.randomUUID()

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isNotFound
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 403 when user does not have permission to view premises in that region`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(bedspaceCount = 1) { premises, rooms, beds ->
          givenATemporaryAccommodationPremisesWithRoomsAndBeds(bedspaceCount = 1) { premises1, rooms, beds1 ->
            val bed = beds.first()

            webTestClient.get()
              .uri("/cas3/premises/${premises1.id}/bedspaces/${bed.id}/can-archive")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isForbidden
          }
        }
      }
    }

    @Test
    fun `Can archive a bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = createPremises(probationRegionEntityFactory.produceAndPersist())
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(360), endDate = null)

        webTestClient.get()
          .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  private fun weekdayAfter(date: LocalDate): LocalDate = when (date.getDayOfWeek()) {
    DayOfWeek.SATURDAY -> date.plusDays(2)
    DayOfWeek.SUNDAY -> date.plusDays(1)
    else -> date
  }

  private fun weekdayBefore(date: LocalDate): LocalDate = when (date.getDayOfWeek()) {
    DayOfWeek.SATURDAY -> date.minusDays(1)
    DayOfWeek.SUNDAY -> date.minusDays(2)
    else -> date
  }

  private companion object {
    @JvmStatic
    fun getArchivedPremisesByIdCases() = listOf(
      Pair(LocalDate.now().minusDays(1), Cas3PremisesStatus.archived),
      Pair(LocalDate.now().plusDays(5), Cas3PremisesStatus.online),
    )
  }
}
