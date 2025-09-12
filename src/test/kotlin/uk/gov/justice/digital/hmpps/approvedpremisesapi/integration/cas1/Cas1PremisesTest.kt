package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toMap
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1NationalOccupancy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1NationalOccupancyParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesController.Cas1CurrentKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesLocalRestrictionSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PremisesNewLocalRestriction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository.Companion.UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_JANITOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class Cas1PremisesTest : IntegrationTestBase() {

  @Nested
  inner class GetPremises : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity
    lateinit var offender: CaseSummary
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementRequest: PlacementRequestEntity
    lateinit var user: UserEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      user = givenAUser().first

      premises = givenAnApprovedPremises(
        name = "the premises name",
        supportsSpaceBookings = true,
        apCode = "the ap code",
        postCode = "the postcode",
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "The ap area name"),
        ),
        managerDetails = "manager details",
        characteristics = listOf(characteristicRepository.findCas1ByPropertyName("isSuitableForVulnerable")!!),
      )

      givenAnApprovedPremisesRoom(
        premises,
        characteristics = listOf(characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!),
      )

      givenAnApprovedPremisesRoom(
        premises,
        characteristics = listOf(characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!),
      )
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 if premise doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @SuppressWarnings("UnusedPrivateProperty")
    @Test
    fun `Returns premises`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      repeat((1..10).count()) {
        givenACas1SpaceBooking(
          crn = "X123",
          premises = premises,
          expectedArrivalDate = now().minusDays(3),
          expectedDepartureDate = now().plusWeeks(14),
        )
      }

      // non arrival, ignored
      givenACas1SpaceBooking(
        crn = "X123",
        premises = premises,
        expectedArrivalDate = now().minusDays(3),
        expectedDepartureDate = now().plusWeeks(14),
        nonArrivalConfirmedAt = Instant.now(),
      )

      // cancelled, ignored
      givenACas1SpaceBooking(
        crn = "X123",
        premises = premises,
        expectedArrivalDate = now().minusDays(3),
        expectedDepartureDate = now().plusWeeks(14),
        cancellationOccurredAt = now(),
      )

      val beds = bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      // cancelled, ignored
      givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = now().minusDays(1),
        endDate = now().plusDays(4),
        cancelled = true,
      )

      // future, ignored
      givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = now().plusDays(2),
        endDate = now().plusDays(4),
      )

      // current
      givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = now().minusDays(2),
        endDate = now().plusDays(2),
      )

      val restriction1 = cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
        withApprovedPremisesId(premises.id)
        withDescription("No offence against sex workers")
        withCreatedAt(OffsetDateTime.now().minusDays(1))
        withCreatedByUserId(user.id)
      }

      val restriction2 = cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
        withApprovedPremisesId(premises.id)
        withDescription("No hate based offences")
        withCreatedAt(OffsetDateTime.now())
        withCreatedByUserId(user.id)
      }

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1Premises>()

      assertThat(summary.id).isEqualTo(premises.id)
      assertThat(summary.name).isEqualTo("the premises name")
      assertThat(summary.apCode).isEqualTo("the ap code")
      assertThat(summary.postcode).isEqualTo("the postcode")
      assertThat(summary.bedCount).isEqualTo(5)
      assertThat(summary.outOfServiceBeds).isEqualTo(1)
      assertThat(summary.availableBeds).isEqualTo(-6)
      assertThat(summary.apArea.name).isEqualTo("The ap area name")
      assertThat(summary.managerDetails).isEqualTo("manager details")
      assertThat(summary.localRestrictions).containsExactly(
        Cas1PremisesLocalRestrictionSummary(restriction2.id, restriction2.description, restriction2.createdAt.toLocalDate()),
        Cas1PremisesLocalRestrictionSummary(restriction1.id, restriction1.description, restriction1.createdAt.toLocalDate()),
      )
      assertThat(summary.characteristics).containsExactlyInAnyOrder(
        Cas1SpaceCharacteristic.isArsonSuitable,
        Cas1SpaceCharacteristic.hasEnSuite,
        Cas1SpaceCharacteristic.isSuitableForVulnerable,
      )
    }
  }

  @Nested
  inner class GetPremisesSummaries : InitialiseDatabasePerClassTestBase() {

    lateinit var premises1ManInArea1: ApprovedPremisesEntity
    lateinit var premises2WomanInArea2: ApprovedPremisesEntity
    lateinit var premises3ManInArea2: ApprovedPremisesEntity
    lateinit var cruManagementArea: Cas1CruManagementAreaEntity

    lateinit var apArea1: ApAreaEntity
    lateinit var apArea2: ApAreaEntity

    @BeforeAll
    fun setupTestData() {
      apArea1 = givenAnApArea(name = "the ap area name 1")
      apArea2 = givenAnApArea(name = "the ap area name 2")

      cruManagementArea = givenACas1CruManagementArea()

      val region1 = givenAProbationRegion(
        apArea = apArea1,
      )

      val region2 = givenAProbationRegion(
        apArea = apArea2,
      )

      premises1ManInArea1 = givenAnApprovedPremises(
        name = "the premises name 1",
        gender = ApprovedPremisesGender.MAN,
        region = region1,
        supportsSpaceBookings = false,
      )

      premises2WomanInArea2 = givenAnApprovedPremises(
        name = "the premises name 2",
        gender = ApprovedPremisesGender.WOMAN,
        region = region2,
        supportsSpaceBookings = false,
      )

      premises3ManInArea2 = givenAnApprovedPremises(
        name = "the premises name 3",
        gender = ApprovedPremisesGender.MAN,
        region = region2,
        supportsSpaceBookings = true,
        cruManagementArea = cruManagementArea,
      )

      givenAnApprovedPremises(
        name = "an archived premises",
        status = PropertyStatus.archived,
        gender = ApprovedPremisesGender.MAN,
        region = region2,
        supportsSpaceBookings = true,
      )

      // premises 1 live beds
      repeat(5) { givenAnApprovedPremisesBed(premises1ManInArea1) }
      // scheduled for removal in the future
      givenAnApprovedPremisesBed(
        premises = premises1ManInArea1,
        endDate = now().plusDays(5),
      )
      // removed bed
      givenAnApprovedPremisesBed(premises1ManInArea1, endDate = now().minusDays(5))
      // bed scheduled for removal today
      givenAnApprovedPremisesBed(premises1ManInArea1, endDate = now())

      // premises 2 live beds
      repeat(2) { givenAnApprovedPremisesBed(premises2WomanInArea2) }
      // Bed scheduled for removal in the future
      givenAnApprovedPremisesBed(premises2WomanInArea2, endDate = now().plusDays(20))

      // premises 3 live beds
      repeat(20) { givenAnApprovedPremisesBed(premises3ManInArea2, endDate = now().minusDays(5)) }
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `No filters applied`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(3)

      assertThat(summaries)
        .anyMatch {
          it.id == premises1ManInArea1.id &&
            it.name == "the premises name 1" &&
            it.apCode == premises1ManInArea1.apCode &&
            it.apArea.name == "the ap area name 1" &&
            it.bedCount == 6 &&
            it.supportsSpaceBookings == false
        }
        .anyMatch {
          it.id == premises2WomanInArea2.id &&
            it.name == "the premises name 2" &&
            it.apCode == premises2WomanInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 3 &&
            it.supportsSpaceBookings == false
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.name == "the premises name 3" &&
            it.apCode == premises3ManInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0 &&
            it.supportsSpaceBookings == true
        }
    }

    @Test
    fun `filter gender is man`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=man")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(2)

      assertThat(summaries)
        .anyMatch {
          it.id == premises1ManInArea1.id &&
            it.name == "the premises name 1" &&
            it.apCode == premises1ManInArea1.apCode &&
            it.apArea.name == "the ap area name 1" &&
            it.bedCount == 6
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.name == "the premises name 3" &&
            it.apCode == premises3ManInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0
        }
    }

    @Test
    fun `filter gender is woman`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=woman")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises2WomanInArea2.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 2")
      assertThat(summaries[0].apCode).isEqualTo(premises2WomanInArea2.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
      assertThat(summaries[0].bedCount).isEqualTo(3)
    }

    @Test
    fun `filter ap area`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?apAreaId=${apArea1.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises1ManInArea1.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 1")
      assertThat(summaries[0].apCode).isEqualTo(premises1ManInArea1.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 1")
      assertThat(summaries[0].bedCount).isEqualTo(6)
    }

    @Test
    fun `filter gender and ap area`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?gender=man&apAreaId=${apArea2.id}&")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises3ManInArea2.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 3")
      assertThat(summaries[0].apCode).isEqualTo(premises3ManInArea2.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `filter by cruManagementId`() {
      val (_, jwt) = givenAUser()

      val summaries = webTestClient.get()
        .uri("/cas1/premises/summary?cruManagementAreaId=${cruManagementArea.id}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1PremisesBasicSummary>()

      assertThat(summaries).hasSize(1)

      assertThat(summaries[0].id).isEqualTo(premises3ManInArea2.id)
      assertThat(summaries[0].name).isEqualTo("the premises name 3")
      assertThat(summaries[0].apCode).isEqualTo(premises3ManInArea2.apCode)
      assertThat(summaries[0].apArea.name).isEqualTo("the ap area name 2")
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }
  }

  /**
   * The [SpacePlanningServiceTest] provides a thorough integration test for
   * calculating capacity. For that reason this integration test only provides
   * a basic success path test
   */
  @Nested
  inner class GetPremisesCapacity : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion(
        apArea = givenAnApArea(name = "The ap area name"),
      )

      premises = givenAnApprovedPremises(
        name = "the premises name",
        apCode = "the ap code",
        postCode = "the postcode",
        managerDetails = "manager details",
      )
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/capacity?startDate=2020-01-01&endDate=2020-01-02")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      val result = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/capacity?startDate=2020-01-01&endDate=2020-01-02")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremiseCapacity::class.java).responseBody.blockFirst()!!

      assertThat(result.capacity).hasSize(2)

      assertThat(result.capacity[0].date).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(result.capacity[1].date).isEqualTo(LocalDate.of(2020, 1, 2))
    }
  }

  @Nested
  inner class GetNationalOccupancy : IntegrationTestBase() {

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = "NN11",
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Mandatory parameters provided, No matching premises returns empty result`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.startDate).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(response.endDate).isEqualTo(LocalDate.of(2025, 1, 7))
      assertThat(response.premises).isEmpty()
    }

    @Test
    fun `Mandatory parameters provided, single premises available and all fields mapped`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      val premises = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises1",
      )

      givenAnApprovedPremisesBed(premises = premises)
      givenAnApprovedPremisesBed(premises = premises)
      givenAnApprovedPremisesBed(premises = premises)

      givenACas1SpaceBooking(
        canonicalArrivalDate = LocalDate.of(2025, 1, 3),
        canonicalDepartureDate = LocalDate.of(2025, 2, 1),
        premises = premises,
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.startDate).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(response.endDate).isEqualTo(LocalDate.of(2025, 1, 7))

      assertThat(response.premises).hasSize(1)
      val premisesResult = response.premises[0]
      assertThat(premisesResult.summary.id).isEqualTo(premises.id)
      assertThat(premisesResult.summary.name).isEqualTo("premises1")

      assertThat(premisesResult.distanceInMiles).isNull()

      val capacities = premisesResult.capacity
      assertThat(capacities).hasSize(7)
      assertThat(capacities[0].date).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(capacities[0].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[0].vacantBedCount).isEqualTo(3)
      assertThat(capacities[1].date).isEqualTo(LocalDate.of(2025, 1, 2))
      assertThat(capacities[1].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[1].vacantBedCount).isEqualTo(3)
      assertThat(capacities[2].date).isEqualTo(LocalDate.of(2025, 1, 3))
      assertThat(capacities[2].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[2].vacantBedCount).isEqualTo(2)
      assertThat(capacities[3].date).isEqualTo(LocalDate.of(2025, 1, 4))
      assertThat(capacities[3].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[3].vacantBedCount).isEqualTo(2)
      assertThat(capacities[4].date).isEqualTo(LocalDate.of(2025, 1, 5))
      assertThat(capacities[4].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[4].vacantBedCount).isEqualTo(2)
      assertThat(capacities[5].date).isEqualTo(LocalDate.of(2025, 1, 6))
      assertThat(capacities[5].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[5].vacantBedCount).isEqualTo(2)
      assertThat(capacities[6].date).isEqualTo(LocalDate.of(2025, 1, 7))
      assertThat(capacities[6].inServiceBedCount).isEqualTo(3)
      assertThat(capacities[6].vacantBedCount).isEqualTo(2)
    }

    @Test
    fun `Postcode not provided, return in order of ap area identifier followed by premises name`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      val regionCC = givenAProbationRegion(apArea = givenAnApArea(identifier = "CC"))

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises 2 in CC",
        region = regionCC,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises 1 in CC",
        region = regionCC,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises in ZZ",
        region = givenAProbationRegion(apArea = givenAnApArea(identifier = "ZZ")),
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises in AA",
        region = givenAProbationRegion(apArea = givenAnApArea(identifier = "AA")),
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.premises).hasSize(4)
      assertThat(response.premises[0].summary.name).isEqualTo("premises in AA")
      assertThat(response.premises[1].summary.name).isEqualTo("premises 1 in CC")
      assertThat(response.premises[2].summary.name).isEqualTo("premises 2 in CC")
      assertThat(response.premises[3].summary.name).isEqualTo("premises in ZZ")
    }

    @Test
    fun `Postcode provided, return in distance order followed by premises name, lowest first`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        postCode = "NN16",
        name = "premises northamptonshire B",
        latitude = 52.407422,
        longitude = -0.700557,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        postCode = "NN16",
        name = "premises northamptonshire A",
        latitude = 52.407422,
        longitude = -0.700557,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        postCode = "SW11",
        name = "premises london",
        latitude = 51.477517,
        longitude = -0.179730,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        postCode = "LE11",
        name = "premises leicestershire",
        latitude = 52.770411,
        longitude = -1.202296,
      )

      postCodeDistrictFactory.produceAndPersist {
        withOutcode("NE1")
        withLatitude(54.967460)
        withLongitude(-1.615383)
      }

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = "NE1",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.premises).hasSize(4)
      assertThat(response.premises[0].summary.name).isEqualTo("premises leicestershire")
      assertThat(response.premises[1].summary.name).isEqualTo("premises northamptonshire A")
      assertThat(response.premises[2].summary.name).isEqualTo("premises northamptonshire B")
      assertThat(response.premises[3].summary.name).isEqualTo("premises london")
    }

    @Test
    fun `Premises and room characteristics provided, ignore room characteristics for premises filtering`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      // no matching chars, ignored
      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        characteristics = emptyList(),
      )

      // only one matching char, ignored
      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("isCatered")!!,
          characteristicRepository.findCas1ByPropertyName("hasWideAccessToCommunalAreas")!!,
        ),
      )

      val matchingPremises = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("isCatered")!!,
          characteristicRepository.findCas1ByPropertyName("acceptsNonSexualChildOffenders")!!,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = setOf(Cas1SpaceCharacteristic.isCatered, Cas1SpaceCharacteristic.acceptsNonSexualChildOffenders),
            roomCharacteristics = setOf(Cas1SpaceCharacteristic.hasEnSuite),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.premises).hasSize(1)
      val premisesResult = response.premises[0]
      assertThat(premisesResult.summary.id).isEqualTo(matchingPremises.id)
    }

    @Test
    fun `Room characteristics provided, filter occupancy results on room characteristics`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      val premises = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        name = "premises1",
      )

      givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
          characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
        ),
      )

      givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
        ),
      )

      givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(
          characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
        ),
      )

      givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = emptyList(),
      )

      givenACas1SpaceBooking(
        canonicalArrivalDate = LocalDate.of(2025, 1, 1),
        canonicalDepartureDate = LocalDate.of(2025, 2, 1),
        premises = premises,
        criteria = listOf(
          characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
        ),
      )

      givenACas1SpaceBooking(
        canonicalArrivalDate = LocalDate.of(2025, 1, 2),
        canonicalDepartureDate = LocalDate.of(2025, 1, 3),
        premises = premises,
        criteria = listOf(
          characteristicRepository.findCas1ByPropertyName("isArsonSuitable")!!,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = emptySet(),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = setOf(
              Cas1SpaceCharacteristic.hasEnSuite,
              Cas1SpaceCharacteristic.isArsonSuitable,
            ),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.startDate).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(response.endDate).isEqualTo(LocalDate.of(2025, 1, 7))

      assertThat(response.premises).hasSize(1)
      val premisesResult = response.premises[0]

      val capacities = premisesResult.capacity
      assertThat(capacities).hasSize(7)
      assertThat(capacities[0].date).isEqualTo(LocalDate.of(2025, 1, 1))
      assertThat(capacities[0].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[0].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[0].vacantBedCount).isEqualTo(0)
      assertThat(capacities[1].date).isEqualTo(LocalDate.of(2025, 1, 2))
      assertThat(capacities[1].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[1].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[1].vacantBedCount).isEqualTo(-1)
      assertThat(capacities[2].date).isEqualTo(LocalDate.of(2025, 1, 3))
      assertThat(capacities[2].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[2].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[2].vacantBedCount).isEqualTo(0)
      assertThat(capacities[3].date).isEqualTo(LocalDate.of(2025, 1, 4))
      assertThat(capacities[3].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[3].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[3].vacantBedCount).isEqualTo(0)
      assertThat(capacities[4].date).isEqualTo(LocalDate.of(2025, 1, 5))
      assertThat(capacities[4].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[4].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[4].vacantBedCount).isEqualTo(0)
      assertThat(capacities[5].date).isEqualTo(LocalDate.of(2025, 1, 6))
      assertThat(capacities[5].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[5].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[5].vacantBedCount).isEqualTo(0)
      assertThat(capacities[6].date).isEqualTo(LocalDate.of(2025, 1, 7))
      assertThat(capacities[6].forRoomCharacteristic).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
      assertThat(capacities[6].inServiceBedCount).isEqualTo(1)
      assertThat(capacities[6].vacantBedCount).isEqualTo(0)
    }

    @Test
    fun `CRU Management Area IDs provided, only return premises in those areas`() {
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_JANITOR))

      val area1 = givenACas1CruManagementArea()
      val area2 = givenACas1CruManagementArea()
      val area3 = givenACas1CruManagementArea()
      val area4 = givenACas1CruManagementArea()

      val area1Premises1 = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        cruManagementArea = area1,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        cruManagementArea = area2,
      )

      val area3Premises1 = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        cruManagementArea = area3,
      )

      val area3Premises2 = givenAnApprovedPremises(
        supportsSpaceBookings = true,
        cruManagementArea = area3,
      )

      givenAnApprovedPremises(
        supportsSpaceBookings = true,
        cruManagementArea = area4,
      )

      val response = webTestClient.post()
        .uri("/cas1/premises/capacity")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1NationalOccupancyParameters(
            fromDate = LocalDate.of(2025, 1, 1),
            cruManagementAreaIds = setOf(area1.id, area3.id),
            premisesCharacteristics = emptySet(),
            roomCharacteristics = emptySet(),
            postcodeArea = null,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1NationalOccupancy>()

      assertThat(response.premises).hasSize(3)
      assertThat(response.premises.map { it.summary.id }).containsExactlyInAnyOrder(
        area1Premises1.id,
        area3Premises1.id,
        area3Premises2.id,
      )
    }
  }

  @Nested
  inner class GetDaySummary : InitialiseDatabasePerClassTestBase() {

    lateinit var user: UserEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBookingEarly: Cas1SpaceBookingEntity
    lateinit var spaceBookingLate: Cas1SpaceBookingEntity
    lateinit var spaceBookingOfflineApplicationKeyWorker: UserEntity
    lateinit var spaceBookingOfflineApplication: Cas1SpaceBookingEntity
    lateinit var applicationA: ApprovedPremisesApplicationEntity
    lateinit var placementRequestA: PlacementRequestEntity
    lateinit var applicationB: ApprovedPremisesApplicationEntity
    lateinit var placementRequestB: PlacementRequestEntity
    lateinit var offenderA: CaseSummary
    lateinit var offenderB: CaseSummary
    lateinit var offenderOffline: CaseSummary
    lateinit var outOfServiceBed: Cas1OutOfServiceBedEntity
    lateinit var outOfServiceBedEndingToday: Cas1OutOfServiceBedEntity
    lateinit var outOfServiceBedTodayOnly: Cas1OutOfServiceBedEntity
    lateinit var isGroundFloorCharacteristic: CharacteristicEntity
    lateinit var isArsonSuitableCharacteristic: CharacteristicEntity
    lateinit var hasEnSuiteCharacteristic: CharacteristicEntity

    val summaryDate: LocalDate = now()
    val tierA = "A2"
    val tierB = "B3"

    @SuppressWarnings("UnusedPrivateProperty", "LongMethod")
    @BeforeAll
    fun setupTestData() {
      user = givenAUser().first

      premises = givenAnApprovedPremises(
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "The ap area name"),
        ),
      )

      setupBookings()
      setupOutOfServiceBeds()
    }

    fun setupBookings() {
      val now = now()

      bedEntityFactory.produceAndPersistMultiple(5) {
        withYieldedRoom {
          roomEntityFactory.produceAndPersist {
            withYieldedPremises { premises }
          }
        }
      }

      offenderA = givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn("crn1")
          withFirstName("firstNameAAA")
          withLastName("lastNameAAA")
        },
      ).first.asCaseSummary()

      val (offenderOfflineApplication) = givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn("offline_crn")
          withFirstName("mister")
          withLastName("offline")
        },
      )
      offenderOffline = offenderOfflineApplication.asCaseSummary()

      offenderB = givenAnOffender(
        offenderDetailsConfigBlock = {
          withCrn("crn2")
          withFirstName("firstNameBBB")
          withLastName("lastNameBBB")
        },
      ).first.asCaseSummary()

      apDeliusContextAddListCaseSummaryToBulkResponse(listOf(offenderA, offenderB, offenderOffline))

      givenAPlacementRequest(
        assessmentAllocatedTo = user,
        createdByUser = user,
        crn = offenderA.crn,
        name = "${offenderA.name.forename} ${offenderA.name.surname}",
        tier = tierA,
      ).let {
        placementRequestA = it.first
        applicationA = it.second
      }

      spaceBookingEarly = createSpaceBooking(
        crn = offenderA.crn,
        placementRequest = this.placementRequestA,
        application = applicationA,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(3))
        withCanonicalDepartureDate(now.plusDays(3))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ARSON_SUITABLE),
        )
      }

      givenAPlacementRequest(
        assessmentAllocatedTo = user,
        createdByUser = user,
        crn = offenderB.crn,
        name = "${offenderB.name.forename} ${offenderB.name.surname}",
        tier = tierB,
      ).let {
        placementRequestB = it.first
        applicationB = it.second
      }

      spaceBookingLate = createSpaceBooking(
        crn = offenderB.crn,
        placementRequest = this.placementRequestA,
        application = applicationB,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(6))
        withCanonicalDepartureDate(now.plusDays(6))
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_ARSON_SUITABLE),
          findCharacteristic(CAS1_PROPERTY_NAME_ENSUITE),
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
      }

      spaceBookingOfflineApplicationKeyWorker = givenAUser().first
      spaceBookingOfflineApplication = createSpaceBookingWithOfflineApplication(
        crn = offenderOffline.crn,
        firstName = offenderOffline.name.forename,
        lastName = offenderOffline.name.surname,
        premises = premises,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(2))
        withCanonicalDepartureDate(now.plusDays(2))
        withActualDepartureDate(null)
        withCriteria(
          findCharacteristic(CAS1_PROPERTY_NAME_SINGLE_ROOM),
        )
        withKeyworkerStaffCode("key worker 1 code")
        withKeyworkerName("key worker 1 name")
        withKeyWorkerUser(spaceBookingOfflineApplicationKeyWorker)
      }

      // cancelled - ignored
      createSpaceBooking(
        crn = offenderB.crn,
        placementRequest = this.placementRequestA,
        application = applicationB,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(2))
        withCanonicalDepartureDate(now.plusDays(2))
        withCancellationOccurredAt(now)
      }

      // non arrival - ignored
      createSpaceBooking(
        crn = offenderB.crn,
        placementRequest = this.placementRequestA,
        application = applicationB,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(2))
        withCanonicalDepartureDate(now.plusDays(2))
        withNonArrivalConfirmedAt(Instant.now())
      }

      // departing on same day - ignored
      createSpaceBooking(
        crn = offenderB.crn,
        placementRequest = this.placementRequestA,
        application = applicationB,
      ) {
        withPremises(premises)
        withCanonicalArrivalDate(now.minusDays(2))
        withCanonicalDepartureDate(now)
      }
    }

    fun setupOutOfServiceBeds() {
      val now = now()

      hasEnSuiteCharacteristic = characteristicRepository.findByPropertyName("hasEnSuite", "approved-premises")!!
      isArsonSuitableCharacteristic = characteristicRepository.findByPropertyName("isArsonSuitable", "approved-premises")!!
      isGroundFloorCharacteristic = characteristicRepository.findByPropertyName("isGroundFloor", "approved-premises")!!

      val bed1 = givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(hasEnSuiteCharacteristic),
      )

      val bed2 = givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(hasEnSuiteCharacteristic, isArsonSuitableCharacteristic),
      )

      val bed3 = givenAnApprovedPremisesBed(
        premises = premises,
        characteristics = listOf(hasEnSuiteCharacteristic, isArsonSuitableCharacteristic, isGroundFloorCharacteristic),
      )

      outOfServiceBed = givenAnOutOfServiceBed(
        bed = bed1,
        startDate = now.minusDays(20),
        endDate = now.plusDays(20),
      )

      outOfServiceBedEndingToday = givenAnOutOfServiceBed(
        bed = bed2,
        startDate = now.minusDays(10),
        endDate = now,
      )

      outOfServiceBedTodayOnly = givenAnOutOfServiceBed(
        bed = bed3,
        startDate = now,
        endDate = now,
      )

      // cancelled, ignored
      givenAnOutOfServiceBed(
        bed = bed3,
        startDate = now,
        endDate = now,
        cancelled = true,
      )

      // expired, ignored
      givenAnOutOfServiceBed(
        bed = bed1,
        startDate = now.minusDays(20),
        endDate = now.minusDays(1),
        cancelled = false,
      )

      // upcoming, ignored
      givenAnOutOfServiceBed(
        bed = bed1,
        startDate = now.plusDays(1),
        endDate = now.plusDays(10),
        cancelled = false,
      )
    }

    @Test
    fun `403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `404 if premise doesn't exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/day-summary/$summaryDate")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `return bookings applicable to given date`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summaries.forDate).isEqualTo(summaryDate)
      assertThat(summaries.nextDate).isEqualTo(summaryDate.plusDays(1))
      assertThat(summaries.previousDate).isEqualTo(summaryDate.minusDays(1))
      assertThat(summaries.outOfServiceBeds.size).isEqualTo(3)

      assertThat(summaries.spaceBookingSummaries).hasSize(3)

      val bookingSummary1 = summaries.spaceBookingSummaries[0]
      assertThat(bookingSummary1.id).isEqualTo(spaceBookingOfflineApplication.id)
      assertThat(bookingSummary1.tier).isEqualTo(null)
      assertThat(bookingSummary1.canonicalArrivalDate).isEqualTo(bookingSummary1.canonicalArrivalDate)
      assertThat(bookingSummary1.canonicalDepartureDate).isEqualTo(bookingSummary1.canonicalDepartureDate)
      assertThat(bookingSummary1.characteristics.size).isEqualTo(1)
      assertThat(bookingSummary1.characteristics[0].value).isEqualTo(CAS1_PROPERTY_NAME_SINGLE_ROOM)

      val bookingSummary1KeyWorkerAllocation = bookingSummary1.keyWorkerAllocation!!
      assertThat(bookingSummary1KeyWorkerAllocation.userId).isEqualTo(spaceBookingOfflineApplicationKeyWorker.id)
      assertThat(bookingSummary1KeyWorkerAllocation.name).isEqualTo("key worker 1 name")
      assertThat(bookingSummary1KeyWorkerAllocation.emailAddress).isEqualTo(spaceBookingOfflineApplicationKeyWorker.email)

      val offender1 = bookingSummary1.person
      assertThat(offender1.crn).isEqualTo(offenderOffline.crn)
      assertThat(offender1.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(offender1).isInstanceOf(FullPersonSummary::class.java)
      assertThat((offender1 as FullPersonSummary).name).isEqualTo("${offenderOffline.name.forename} ${offenderOffline.name.surname}")

      val bookingSummary2 = summaries.spaceBookingSummaries[1]
      assertThat(bookingSummary2.id).isEqualTo(spaceBookingLate.id)
      assertThat(bookingSummary2.tier).isEqualTo(tierB)
      assertThat(bookingSummary2.canonicalArrivalDate).isEqualTo(spaceBookingLate.canonicalArrivalDate)
      assertThat(bookingSummary2.canonicalDepartureDate).isEqualTo(spaceBookingLate.canonicalDepartureDate)
      assertThat(bookingSummary2.characteristics.size).isEqualTo(3)
      assertThat(bookingSummary2.characteristics.map { it.value }).containsExactlyInAnyOrder(
        CAS1_PROPERTY_NAME_SINGLE_ROOM,
        CAS1_PROPERTY_NAME_ENSUITE,
        CAS1_PROPERTY_NAME_ARSON_SUITABLE,
      )

      val offender2 = bookingSummary2.person
      assertThat(offender2.crn).isEqualTo(offenderB.crn)
      assertThat(offender2.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(offender2).isInstanceOf(FullPersonSummary::class.java)
      assertThat((offender2 as FullPersonSummary).name).isEqualTo("${offenderB.name.forename} ${offenderB.name.surname}")

      val bookingSummary3 = summaries.spaceBookingSummaries[2]
      assertThat(bookingSummary3.id).isEqualTo(spaceBookingEarly.id)
      assertThat(bookingSummary3.tier).isEqualTo(tierA)
      assertThat(bookingSummary3.canonicalArrivalDate).isEqualTo(spaceBookingEarly.canonicalArrivalDate)
      assertThat(bookingSummary3.canonicalDepartureDate).isEqualTo(spaceBookingEarly.canonicalDepartureDate)
      assertThat(bookingSummary3.characteristics.size).isEqualTo(1)
      assertThat(bookingSummary3.characteristics.map { it.value }).containsExactlyInAnyOrder(
        CAS1_PROPERTY_NAME_ARSON_SUITABLE,
      )

      val offender3 = bookingSummary3.person
      assertThat(offender3.crn).isEqualTo(offenderA.crn)
      assertThat(offender3.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(offender3).isInstanceOf(FullPersonSummary::class.java)
      assertThat((offender3 as FullPersonSummary).name).isEqualTo("${offenderA.name.forename} ${offenderA.name.surname}")
    }

    @Test
    fun `return out of service beds applicable to given date`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      val oosBedSummary = summaries.outOfServiceBeds[0]
      assertThat(oosBedSummary.id).isEqualTo(outOfServiceBed.id)
      assertThat(oosBedSummary.startDate).isEqualTo(outOfServiceBed.startDate)
      assertThat(oosBedSummary.endDate).isEqualTo(outOfServiceBed.endDate)
      assertThat(oosBedSummary.roomName).isEqualTo(outOfServiceBed.bed.room.name)
      assertThat(oosBedSummary.reason.id).isEqualTo(outOfServiceBed.reason.id)
      assertThat(oosBedSummary.reason.name).isEqualTo(outOfServiceBed.reason.name)
      assertThat(oosBedSummary.characteristics.size).isEqualTo(1)
      assertThat(oosBedSummary.characteristics[0].value).isEqualTo(hasEnSuiteCharacteristic.propertyName)

      val oosBedEndingToday = summaries.outOfServiceBeds[1]
      assertThat(oosBedEndingToday.id).isEqualTo(outOfServiceBedEndingToday.id)
      assertThat(oosBedEndingToday.startDate).isEqualTo(outOfServiceBedEndingToday.startDate)
      assertThat(oosBedEndingToday.endDate).isEqualTo(outOfServiceBedEndingToday.endDate)
      assertThat(oosBedEndingToday.roomName).isEqualTo(outOfServiceBedEndingToday.bed.room.name)
      assertThat(oosBedEndingToday.reason.id).isEqualTo(outOfServiceBedEndingToday.reason.id)
      assertThat(oosBedEndingToday.reason.name).isEqualTo(outOfServiceBedEndingToday.reason.name)
      assertThat(oosBedEndingToday.characteristics.map { it.value }).containsExactlyInAnyOrder(
        hasEnSuiteCharacteristic.propertyName,
        isArsonSuitableCharacteristic.propertyName,
      )

      val oosBedTodayOnly = summaries.outOfServiceBeds[2]
      assertThat(oosBedTodayOnly.id).isEqualTo(outOfServiceBedTodayOnly.id)
      assertThat(oosBedTodayOnly.startDate).isEqualTo(outOfServiceBedTodayOnly.startDate)
      assertThat(oosBedTodayOnly.endDate).isEqualTo(outOfServiceBedTodayOnly.endDate)
      assertThat(oosBedTodayOnly.roomName).isEqualTo(outOfServiceBedTodayOnly.bed.room.name)
      assertThat(oosBedTodayOnly.reason.id).isEqualTo(outOfServiceBedTodayOnly.reason.id)
      assertThat(oosBedTodayOnly.reason.name).isEqualTo(outOfServiceBedTodayOnly.reason.name)
      assertThat(oosBedTodayOnly.characteristics.map { it.value }).containsExactlyInAnyOrder(
        hasEnSuiteCharacteristic.propertyName,
        isArsonSuitableCharacteristic.propertyName,
        isGroundFloorCharacteristic.propertyName,
      )
    }

    @Test
    fun `filter on criteria, has matching bookings`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?bookingsCriteriaFilter=hasEnSuite")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summary.forDate).isEqualTo(summaryDate)
      assertThat(summary.nextDate).isEqualTo(summaryDate.plusDays(1))
      assertThat(summary.previousDate).isEqualTo(summaryDate.minusDays(1))
      assertThat(summary.spaceBookingSummaries).hasSize(1)
      val summaryBooking = summary.spaceBookingSummaries[0]
      assertThat(summaryBooking.characteristics.map { it.value }).containsExactlyInAnyOrder(
        CAS1_PROPERTY_NAME_ENSUITE,
        CAS1_PROPERTY_NAME_ARSON_SUITABLE,
        CAS1_PROPERTY_NAME_SINGLE_ROOM,
      )
      assertThat(summary.outOfServiceBeds.size).isEqualTo(3)
    }

    @Test
    fun `filter on criteria, has no matching bookings`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summary = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?bookingsCriteriaFilter=isStepFreeDesignated")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summary.forDate).isEqualTo(summaryDate)
      assertThat(summary.nextDate).isEqualTo(summaryDate.plusDays(1))
      assertThat(summary.previousDate).isEqualTo(summaryDate.minusDays(1))
      assertThat(summary.spaceBookingSummaries).isEmpty()
      assertThat(summary.outOfServiceBeds.size).isEqualTo(3)
    }

    @Test
    fun `exclude specified booking by ID`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val excludeSpaceBookingId: String = URLEncoder.encode(spaceBookingEarly.id.toString(), "UTF-8")

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?excludeSpaceBookingId=$excludeSpaceBookingId")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      val spaceBookingsAvailableInPremisesSummary = summaries.spaceBookingSummaries
      assertThat(spaceBookingsAvailableInPremisesSummary.size).isEqualTo(2)
      assertThat(spaceBookingsAvailableInPremisesSummary).extracting("id").doesNotContain(excludeSpaceBookingId)

      assertThat(summaries.spaceBookingSummaries.map { it.id }).containsExactly(
        spaceBookingOfflineApplication.id,
        spaceBookingLate.id,
      )
    }

    @Test
    fun `sort by person name ascending`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?bookingsSortDirection=asc&bookingsSortBy=personName")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summaries.spaceBookingSummaries).hasSize(3)

      val offender1 = summaries.spaceBookingSummaries[0].person
      assertThat((offender1 as FullPersonSummary).name).isEqualTo("firstNameAAA lastNameAAA")
      val offender2 = summaries.spaceBookingSummaries[1].person
      assertThat((offender2 as FullPersonSummary).name).isEqualTo("firstNameBBB lastNameBBB")
      val offender3 = summaries.spaceBookingSummaries[2].person
      assertThat((offender3 as FullPersonSummary).name).isEqualTo("mister offline")
    }

    @Test
    fun `sort by tier ascending`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?bookingsSortDirection=asc&bookingsSortBy=tier")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summaries.spaceBookingSummaries).hasSize(3)

      assertThat(summaries.spaceBookingSummaries[0].tier).isEqualTo(tierA)
      assertThat(summaries.spaceBookingSummaries[1].tier).isEqualTo(tierB)
      assertThat(summaries.spaceBookingSummaries[2].tier).isNull()
    }

    @Test
    fun `sort by tier descending`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate?bookingsSortDirection=desc&bookingsSortBy=tier")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      assertThat(summaries.spaceBookingSummaries).hasSize(3)

      assertThat(summaries.spaceBookingSummaries[0].tier).isNull()
      assertThat(summaries.spaceBookingSummaries[1].tier).isEqualTo(tierB)
      assertThat(summaries.spaceBookingSummaries[2].tier).isEqualTo(tierA)
    }

    private fun findCharacteristic(propertyName: String) = characteristicRepository.findByPropertyName(propertyName, ServiceName.approvedPremises.value)!!

    private fun createSpaceBooking(
      crn: String,
      placementRequest: PlacementRequestEntity,
      application: ApprovedPremisesApplicationEntity,
      configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
    ): Cas1SpaceBookingEntity {
      val (user) = givenAUser()

      return cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(crn)
        withPlacementRequest(placementRequest)
        withApplication(application)
        withCreatedBy(user)

        configuration.invoke(this)
      }
    }

    private fun createSpaceBookingWithOfflineApplication(
      crn: String,
      firstName: String,
      lastName: String,
      premises: ApprovedPremisesEntity,
      configuration: Cas1SpaceBookingEntityFactory.() -> Unit,
    ): Cas1SpaceBookingEntity {
      val (user) = givenAUser()
      val offlineApplication = givenAnOfflineApplication(
        crn = crn,
        name = "$firstName $lastName",
      )
      return cas1SpaceBookingEntityFactory.produceAndPersist {
        withCrn(crn)
        withPremises(premises)
        withPlacementRequest(null)
        withApplication(null)
        withOfflineApplication(offlineApplication)
        withCreatedBy(user)

        configuration.invoke(this)
      }
    }
  }

  @Nested
  inner class CapacityReport : InitialiseDatabasePerClassTestBase() {

    @BeforeAll
    fun setup() {
      val premises1 = givenAnApprovedPremises(
        name = "Premises 1",
        supportsSpaceBookings = true,
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "Region 1"),
        ),
        cruManagementArea = givenACas1CruManagementArea(
          name = "Management Area 1",
        ),
      )
      val premises2 = givenAnApprovedPremises(
        name = "Premises 2",
        supportsSpaceBookings = true,
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "Region 2"),
        ),
        cruManagementArea = givenACas1CruManagementArea(
          name = "Management Area 2",
        ),
      )
      givenAnApprovedPremises(name = "Premises 3 Ignored", supportsSpaceBookings = false)

      val premises1Beds = (1..9).map { givenAnApprovedPremisesBed(premises1) }
      givenAnApprovedPremisesBed(premises1, endDate = LocalDate.of(2025, 5, 20))

      repeat(35) { givenAnApprovedPremisesBed(premises2) }

      // starting and ending within the month, use latest revision
      givenAnOutOfServiceBed(
        bed = premises1Beds[0],
        startDate = LocalDate.of(2025, 5, 5),
        endDate = LocalDate.of(2025, 5, 27),
      ).apply {
        this.revisionHistory += cas1OutOfServiceBedRevisionEntityFactory.produceAndPersist {
          withCreatedBy(givenAUser().first)
          withCreatedAt(OffsetDateTime.now().plusDays(1))
          withOutOfServiceBed(this@apply)
          withStartDate(LocalDate.of(2025, 5, 15))
          withEndDate(LocalDate.of(2025, 5, 25))
          withReason(cas1OutOfServiceBedReasonEntityFactory.produceAndPersist())
        }
      }

      // spanning the month
      givenAnOutOfServiceBed(
        bed = premises1Beds[0],
        startDate = LocalDate.of(2025, 3, 15),
        endDate = LocalDate.of(2026, 5, 25),
      )

      // cancelled, so ignored
      givenAnOutOfServiceBed(
        bed = premises1Beds[0],
        startDate = LocalDate.of(2025, 5, 15),
        endDate = LocalDate.of(2025, 5, 25),
        cancelled = true,
      )

      givenACas1SpaceBooking(
        crn = "bookingSpanningTheWholeMonth",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 4, 30),
        canonicalDepartureDate = LocalDate.of(2025, 6, 1),
      )

      givenACas1SpaceBooking(
        crn = "bookingStartingInTheMonth",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 5, 27),
        canonicalDepartureDate = LocalDate.of(2025, 6, 1),
      )

      givenACas1SpaceBooking(
        crn = "bookingEndingInTheMonth",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 1, 1),
        canonicalDepartureDate = LocalDate.of(2025, 5, 8),
      )

      givenACas1SpaceBooking(
        crn = "bookingStartingAndEndingInTheMonth",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 5, 5),
        canonicalDepartureDate = LocalDate.of(2025, 5, 10),
      )

      givenACas1SpaceBooking(
        crn = "nonArrivalSoBookingIsIgnored",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 5, 7),
        canonicalDepartureDate = LocalDate.of(2025, 5, 29),
        nonArrivalConfirmedAt = Instant.now(),
      )

      givenACas1SpaceBooking(
        crn = "cancelledSoBookingIsIgnored",
        premises = premises1,
        canonicalArrivalDate = LocalDate.of(2025, 5, 6),
        canonicalDepartureDate = LocalDate.of(2025, 5, 28),
        cancellationOccurredAt = now(),
      )
    }

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.get()
        .uri("/cas1/premises/occupancy-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun success() {
      clock.setNow(LocalDateTime.parse("2025-05-01T10:15:30"))

      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      webTestClient.get()
        .uri("/cas1/premises/occupancy-report")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch("content-disposition", "attachment; filename=\"premises-occupancy-20250501_1015.csv\"")
        .expectBody()
        .consumeWith { response ->
          val actual = DataFrame
            .readCSV(response.responseBody!!.inputStream())
            .toMap()

          assertThat(actual.size).isEqualTo(31)

          assertThat(actual["row_name"]).isEqualTo(listOf("Management Area 1 - Premises 1", "Management Area 2 - Premises 2"))

          fun getDateToOccupancyMap(index: Int) = actual.keys
            .filter { it != "row_name" }.associateWith { date -> (actual[date]!![index] as Int) }

          val premises1DayValues = getDateToOccupancyMap(0)
          val premises2DayValues = getDateToOccupancyMap(1)

          assertThat(premises1DayValues).isEqualTo(
            mapOf(
              // we have 10 beds in the premise
              // bookingEndingInTheMonth has already started
              // bookingSpanningTheWholeMonth has already started
              // one bed is out of service for the whole month
              "1/5" to 7,
              "2/5" to 7,
              "3/5" to 7,
              "4/5" to 7,
              // bookingStartingAndEndingInTheMonth starts
              "5/5" to 6,
              "6/5" to 6,
              "7/5" to 6,
              // bookingEndingInTheMonth ends
              "8/5" to 7,
              "9/5" to 7,
              // bookingStartingAndEndingInTheMonth ends
              "10/5" to 8,
              "11/5" to 8,
              "12/5" to 8,
              "13/5" to 8,
              "14/5" to 8,
              // OOSB starts
              "15/5" to 7,
              "16/5" to 7,
              "17/5" to 7,
              "18/5" to 7,
              "19/5" to 7,
              // Bed reaches end date
              "20/5" to 6,
              "21/5" to 6,
              "22/5" to 6,
              "23/5" to 6,
              "24/5" to 6,
              "25/5" to 6,
              // OOSB ends
              "26/5" to 7,
              // bookingStartingInTheMonth starts
              "27/5" to 6,
              "28/5" to 6,
              "29/5" to 6,
              "30/5" to 6,
            ),
          )

          assertThat(premises2DayValues).isEqualTo(
            mapOf(
              "1/5" to 35,
              "2/5" to 35,
              "3/5" to 35,
              "4/5" to 35,
              "5/5" to 35,
              "6/5" to 35,
              "7/5" to 35,
              "8/5" to 35,
              "9/5" to 35,
              "10/5" to 35,
              "11/5" to 35,
              "12/5" to 35,
              "13/5" to 35,
              "14/5" to 35,
              "15/5" to 35,
              "16/5" to 35,
              "17/5" to 35,
              "18/5" to 35,
              "19/5" to 35,
              "20/5" to 35,
              "21/5" to 35,
              "22/5" to 35,
              "23/5" to 35,
              "24/5" to 35,
              "25/5" to 35,
              "26/5" to 35,
              "27/5" to 35,
              "28/5" to 35,
              "29/5" to 35,
              "30/5" to 35,
            ),
          )
        }
    }
  }

  @Nested
  inner class CreateLocalRestrictions : InitialiseDatabasePerClassTestBase() {

    @Test
    fun `Returns 403 Forbidden if user does not have correct role`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_ASSESSOR))

      webTestClient.post()
        .uri("/cas1/premises/${UUID.randomUUID()}/local-restrictions")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1PremisesNewLocalRestriction(
            description = "description",
          ),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Returns 404 not found if premises does not exist`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_JANITOR))

      webTestClient.post()
        .uri("/cas1/premises/${UUID.randomUUID()}/local-restrictions")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1PremisesNewLocalRestriction(
            description = "description",
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Successfully added restriction for a given premises`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_JANITOR))

      val premises = givenAnApprovedPremises(
        name = "Premises 1",
        supportsSpaceBookings = true,
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "Region 1"),
        ),
        cruManagementArea = givenACas1CruManagementArea(
          name = "Management Area 1",
        ),
      )

      webTestClient.post()
        .uri("/cas1/premises/${premises.id}/local-restrictions")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          Cas1PremisesNewLocalRestriction(
            description = "No hate based offences",
          ),
        )
        .exchange()
        .expectStatus()
        .isNoContent

      val premiseRestrictions = cas1PremisesLocalRestrictionRepository.findAllByApprovedPremisesId(premises.id)

      assertThat(premiseRestrictions.size).isEqualTo(1)
      assertThat(premiseRestrictions.first().description).isEqualTo("No hate based offences")
    }
  }

  @Test
  fun `Should retrieve only non-archived restrictions in created at descending order for a given premises`() {
    val (user, jwt) = givenAUser(roles = listOf(CAS1_JANITOR))

    val premises = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
      region = givenAProbationRegion(
        apArea = givenAnApArea(name = "Region 1"),
      ),
      cruManagementArea = givenACas1CruManagementArea(
        name = "Management Area 1",
      ),
    )

    cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
      withApprovedPremisesId(premises.id)
      withDescription("No child RSo")
      withCreatedAt(OffsetDateTime.now().minusDays(2))
      withCreatedByUserId(user.id)
    }

    cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
      withApprovedPremisesId(premises.id)
      withDescription("No offence against sex workers")
      withCreatedAt(OffsetDateTime.now().minusDays(1))
      withCreatedByUserId(user.id)
    }

    cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
      withApprovedPremisesId(premises.id)
      withDescription("No hate based offences")
      withCreatedAt(OffsetDateTime.now())
      withCreatedByUserId(user.id)
    }

    cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
      withApprovedPremisesId(premises.id)
      withDescription("No orson offenses")
      withCreatedAt(OffsetDateTime.now().minusDays(2))
      withCreatedByUserId(user.id)
      withArchived(true)
    }

    val response = webTestClient.get()
      .uri("/cas1/premises/${premises.id}/local-restrictions")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsListOfObjects<Cas1PremisesLocalRestrictionSummary>()

    assertThat(response.size).isEqualTo(3)
    assertThat(response.map { it.description }).containsExactly(
      "No hate based offences",
      "No offence against sex workers",
      "No child RSo",
    )
  }

  @Test
  fun `Should archive restriction when delete is called`() {
    val (user, jwt) = givenAUser(roles = listOf(CAS1_JANITOR))

    val premises = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
      region = givenAProbationRegion(
        apArea = givenAnApArea(name = "Region 1"),
      ),
      cruManagementArea = givenACas1CruManagementArea(
        name = "Management Area 1",
      ),
    )

    val restriction = cas1PremisesLocalRestrictionEntityFactory.produceAndPersist {
      withApprovedPremisesId(premises.id)
      withDescription("No hate based offences")
      withCreatedAt(OffsetDateTime.now().minusDays(1))
      withCreatedByUserId(user.id)
    }

    val isRestrictionActiveBeforeDelete =
      cas1PremisesLocalRestrictionRepository.findByIdOrNull(restriction.id)
    assertThat(isRestrictionActiveBeforeDelete).isNotNull()
    assertThat(isRestrictionActiveBeforeDelete?.archived).isFalse()

    webTestClient.delete()
      .uri("/cas1/premises/${premises.id}/local-restrictions/${restriction.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNoContent

    val isRestrictionArchivedAfterDelete =
      cas1PremisesLocalRestrictionRepository.findByIdOrNull(restriction.id)
    assertThat(isRestrictionArchivedAfterDelete).isNotNull()
    assertThat(isRestrictionArchivedAfterDelete?.archived).isTrue()
  }

  @Nested
  inner class GetCurrentKeyWorkers {
    @Test
    fun `no bookings, return nothing`() {
      val premises = givenAnApprovedPremises()

      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))
      val result = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/current-key-workers")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1CurrentKeyWorker>()

      assertThat(result).isEmpty()
    }

    @Test
    fun `mix of current and upcoming bookings for multiple keyworkers`() {
      val (keyWorker1, _) = givenAUser()
      val (keyWorker2, _) = givenAUser()

      val premises = givenAnApprovedPremises()
      val otherPremises = givenAnApprovedPremises()

      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null)
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null)
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = now())

      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker2, actualArrivalDate = now())
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker2, actualArrivalDate = now())

      // ignored as not current or upcoming, or different premises
      givenACas1SpaceBooking(premises = otherPremises, keyWorkerUser = keyWorker1, actualArrivalDate = null)
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null, expectedDepartureDate = UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE.minusDays(1))
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null, actualDepartureDate = now())
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null, cancellationOccurredAt = now())
      givenACas1SpaceBooking(premises = premises, keyWorkerUser = keyWorker1, actualArrivalDate = null, nonArrivalConfirmedAt = Instant.now())

      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))
      val result = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/current-key-workers")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas1CurrentKeyWorker>()

      assertThat(result).hasSize(2)

      val keyWorker1Result = result.first { it.summary.id == keyWorker1.id }
      assertThat(keyWorker1Result.summary.name).isEqualTo(keyWorker1.name)
      assertThat(keyWorker1Result.upcomingBookingCount).isEqualTo(2)
      assertThat(keyWorker1Result.currentBookingCount).isEqualTo(1)

      val keyWorker2Result = result.first { it.summary.id == keyWorker2.id }
      assertThat(keyWorker2Result.summary.name).isEqualTo(keyWorker2.name)
      assertThat(keyWorker2Result.upcomingBookingCount).isEqualTo(0)
      assertThat(keyWorker2Result.currentBookingCount).isEqualTo(2)
    }
  }
}
