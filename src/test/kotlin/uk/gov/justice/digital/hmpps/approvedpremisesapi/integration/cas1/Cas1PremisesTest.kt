package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.toMap
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OverbookingRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremisesBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_ASSESSOR
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole.CAS1_FUTURE_MANAGER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class Cas1PremisesTest : IntegrationTestBase() {

  @Nested
  inner class GetPremisesSummary : InitialiseDatabasePerClassTestBase() {

    lateinit var premises: ApprovedPremisesEntity
    lateinit var offender: CaseSummary
    lateinit var application: ApprovedPremisesApplicationEntity
    lateinit var placementRequest: PlacementRequestEntity
    lateinit var user: UserEntity
    lateinit var spaceBooking: Cas1SpaceBookingEntity

    @BeforeAll
    fun setupTestData() {
      user = givenAUser().first
      val region = givenAProbationRegion(
        apArea = givenAnApArea(name = "The ap area name"),
      )

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withApCode("the ap code")
        withPostcode("the postcode")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withManagerDetails("manager details")
        withSupportsSpaceBookings(true)
      }
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
    fun `Returns premises summary`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_FUTURE_MANAGER))

      repeat((1..10).count()) {
        givenACas1SpaceBooking(
          crn = "X123",
          premises = premises,
          expectedArrivalDate = LocalDate.now().minusDays(3),
          expectedDepartureDate = LocalDate.now().plusWeeks(14),
        )
      }

      // non arrival, ignored
      givenACas1SpaceBooking(
        crn = "X123",
        premises = premises,
        expectedArrivalDate = LocalDate.now().minusDays(3),
        expectedDepartureDate = LocalDate.now().plusWeeks(14),
        nonArrivalConfirmedAt = Instant.now(),
      )

      // cancelled, ignored
      givenACas1SpaceBooking(
        crn = "X123",
        premises = premises,
        expectedArrivalDate = LocalDate.now().minusDays(3),
        expectedDepartureDate = LocalDate.now().plusWeeks(14),
        cancellationOccurredAt = LocalDate.now(),
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
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now().plusDays(4),
        cancelled = true,
      )

      // future, ignored
      givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = LocalDate.now().plusDays(2),
        endDate = LocalDate.now().plusDays(4),
      )

      // current
      givenAnOutOfServiceBed(
        bed = beds[0],
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now().plusDays(2),
      )

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
      assertThat(summary.overbookingSummary).containsExactly(
        Cas1OverbookingRange(LocalDate.now(), LocalDate.now().plusWeeks(12).minusDays(1)),
      )
    }
  }

  @Nested
  inner class GetPremisesSummaries : InitialiseDatabasePerClassTestBase() {

    lateinit var premises1ManInArea1: ApprovedPremisesEntity
    lateinit var premises2WomanInArea2: ApprovedPremisesEntity
    lateinit var premises3ManInArea2: ApprovedPremisesEntity

    lateinit var apArea1: ApAreaEntity
    lateinit var apArea2: ApAreaEntity

    @BeforeAll
    fun setupTestData() {
      apArea1 = givenAnApArea(name = "the ap area name 1")
      apArea2 = givenAnApArea(name = "the ap area name 2")

      val region1 = givenAProbationRegion(
        apArea = apArea1,
      )

      val region2 = givenAProbationRegion(
        apArea = apArea2,
      )

      premises1ManInArea1 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 1")
        withGender(ApprovedPremisesGender.MAN)
        withYieldedProbationRegion { region1 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(false)
      }

      premises2WomanInArea2 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 2")
        withGender(ApprovedPremisesGender.WOMAN)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(false)
      }

      premises3ManInArea2 = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name 3")
        withGender(ApprovedPremisesGender.MAN)
        withYieldedProbationRegion { region2 }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withSupportsSpaceBookings(true)
      }
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `Returns premises summaries with no filters applied`() {
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
            it.bedCount == 0 &&
            it.supportsSpaceBookings == false
        }
        .anyMatch {
          it.id == premises2WomanInArea2.id &&
            it.name == "the premises name 2" &&
            it.apCode == premises2WomanInArea2.apCode &&
            it.apArea.name == "the ap area name 2" &&
            it.bedCount == 0 &&
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
    fun `Returns premises summaries where gender is man`() {
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
            it.bedCount == 0
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
    fun `Returns premises summaries where gender is woman`() {
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
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `Returns premises summaries for specified ap area`() {
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
      assertThat(summaries[0].bedCount).isEqualTo(0)
    }

    @Test
    fun `Returns premises summaries for specified gender and ap area`() {
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
    fun `Returns correct bed count for premise summaries`() {
      val (_, jwt) = givenAUser()

      val premises1ManRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises1ManInArea1 }
      }
      val premises2WomanRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises2WomanInArea2 }
      }
      val premises3ManRoom = roomEntityFactory.produceAndPersist {
        withYieldedPremises { premises3ManInArea2 }
      }

      val premises1ManLiveBeds = listOf(
        bedEntityFactory.produceAndPersistMultiple(5) {
          withYieldedRoom { premises1ManRoom }
        },
        // Beds scheduled for removal in the future
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises1ManRoom }
          withEndDate { LocalDate.now().plusDays(5) }
        },
      ).flatten()
      // Removed beds
      bedEntityFactory.produceAndPersistMultiple(2) {
        withYieldedRoom { premises1ManRoom }
        withEndDate { LocalDate.now().minusDays(5) }
      }
      // Beds scheduled for removal today
      bedEntityFactory.produceAndPersistMultiple(3) {
        withYieldedRoom { premises1ManRoom }
        withEndDate { LocalDate.now() }
      }

      val premises2WomanLiveBeds = listOf(
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises2WomanRoom }
        },
        // Beds scheduled for removal in the future
        bedEntityFactory.produceAndPersistMultiple(2) {
          withYieldedRoom { premises2WomanRoom }
          withEndDate { LocalDate.now().plusDays(20) }
        },
      ).flatten()

      bedEntityFactory.produceAndPersistMultiple(20) {
        withYieldedRoom { premises3ManRoom }
        withEndDate { LocalDate.now().minusDays(5) }
      }

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
            it.bedCount == premises1ManLiveBeds.count()
        }
        .anyMatch {
          it.id == premises2WomanInArea2.id &&
            it.bedCount == premises2WomanLiveBeds.count()
        }
        .anyMatch {
          it.id == premises3ManInArea2.id &&
            it.bedCount == 0
        }
    }
  }

  @Nested
  inner class GetCapacity : InitialiseDatabasePerClassTestBase() {
    lateinit var premises: ApprovedPremisesEntity

    @BeforeAll
    fun setupTestData() {
      val region = givenAProbationRegion(
        apArea = givenAnApArea(name = "The ap area name"),
      )

      premises = approvedPremisesEntityFactory.produceAndPersist {
        withName("the premises name")
        withApCode("the ap code")
        withPostcode("the postcode")
        withYieldedProbationRegion { region }
        withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
        withManagerDetails("manager details")
      }
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
  inner class GetDaySummary : InitialiseDatabasePerClassTestBase() {

    lateinit var user: UserEntity
    lateinit var premises: ApprovedPremisesEntity
    lateinit var spaceBookingEarly: Cas1SpaceBookingEntity
    lateinit var spaceBookingLate: Cas1SpaceBookingEntity
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

    val summaryDate: LocalDate = LocalDate.now()
    val tierA = "A2"
    val tierB = "B3"
    val releaseTypeToBeDetermined = "TBD"

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
      val now = LocalDate.now()

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
        placementRequestAllocatedTo = user,
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
        placementRequestAllocatedTo = user,
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
      val now = LocalDate.now()

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
      assertThat(summaries.spaceBookings.size).isEqualTo(3)
      assertThat(summaries.spaceBookings.size).isEqualTo(3)
      assertThat(summaries.outOfServiceBeds.size).isEqualTo(3)

      assertThat(summaries.spaceBookings).hasSize(3)

      val summaryBookingOffline = summaries.spaceBookings[0]
      assertThat(summaryBookingOffline.id).isEqualTo(spaceBookingOfflineApplication.id)
      assertThat(summaryBookingOffline.tier).isEqualTo(null)
      assertThat(summaryBookingOffline.releaseType).isEqualTo(releaseTypeToBeDetermined)
      assertThat(summaryBookingOffline.canonicalArrivalDate).isEqualTo(summaryBookingOffline.canonicalArrivalDate)
      assertThat(summaryBookingOffline.canonicalDepartureDate).isEqualTo(summaryBookingOffline.canonicalDepartureDate)
      assertThat(summaryBookingOffline.essentialCharacteristics.size).isEqualTo(1)
      assertThat(summaryBookingOffline.essentialCharacteristics[0].value).isEqualTo(CAS1_PROPERTY_NAME_SINGLE_ROOM)

      val summaryBookingOfflineOffender = summaryBookingOffline.person
      assertThat(summaryBookingOfflineOffender.crn).isEqualTo(offenderOffline.crn)
      assertThat(summaryBookingOfflineOffender.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(summaryBookingOfflineOffender).isInstanceOf(FullPersonSummary::class.java)
      assertThat((summaryBookingOfflineOffender as FullPersonSummary).name).isEqualTo("${offenderOffline.name.forename} ${offenderOffline.name.surname}")

      val summaryBooking1 = summaries.spaceBookings[1]
      assertThat(summaryBooking1.id).isEqualTo(spaceBookingLate.id)
      assertThat(summaryBooking1.tier).isEqualTo(tierB)
      assertThat(summaryBooking1.canonicalArrivalDate).isEqualTo(spaceBookingLate.canonicalArrivalDate)
      assertThat(summaryBooking1.canonicalDepartureDate).isEqualTo(spaceBookingLate.canonicalDepartureDate)
      assertThat(summaryBooking1.essentialCharacteristics.size).isEqualTo(3)
      assertThat(summaryBooking1.essentialCharacteristics.map { it.value }).containsExactlyInAnyOrder(
        CAS1_PROPERTY_NAME_SINGLE_ROOM,
        CAS1_PROPERTY_NAME_ENSUITE,
        CAS1_PROPERTY_NAME_ARSON_SUITABLE,
      )

      val summaryBooking1Offender = summaryBooking1.person
      assertThat(summaryBooking1Offender.crn).isEqualTo(offenderB.crn)
      assertThat(summaryBooking1Offender.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(summaryBooking1Offender).isInstanceOf(FullPersonSummary::class.java)
      assertThat((summaryBooking1Offender as FullPersonSummary).name).isEqualTo("${offenderB.name.forename} ${offenderB.name.surname}")

      val summaryBooking2 = summaries.spaceBookings[2]
      assertThat(summaryBooking2.id).isEqualTo(spaceBookingEarly.id)
      assertThat(summaryBooking2.tier).isEqualTo(tierA)
      assertThat(summaryBooking2.canonicalArrivalDate).isEqualTo(spaceBookingEarly.canonicalArrivalDate)
      assertThat(summaryBooking2.canonicalDepartureDate).isEqualTo(spaceBookingEarly.canonicalDepartureDate)
      assertThat(summaryBooking2.essentialCharacteristics.size).isEqualTo(1)
      assertThat(summaryBooking2.essentialCharacteristics[0].value).isEqualTo(CAS1_PROPERTY_NAME_ARSON_SUITABLE)
      assertThat(summaryBooking2.releaseType).isEqualTo(releaseTypeToBeDetermined)

      val summaryBookingOffender = summaryBooking2.person
      assertThat(summaryBookingOffender.crn).isEqualTo(offenderA.crn)
      assertThat(summaryBookingOffender.personType).isEqualTo(PersonSummaryDiscriminator.fullPersonSummary)
      assertThat(summaryBookingOffender).isInstanceOf(FullPersonSummary::class.java)
      assertThat((summaryBookingOffender as FullPersonSummary).name).isEqualTo("${offenderA.name.forename} ${offenderA.name.surname}")
    }

    @Test
    fun `return capacity information`() {
      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER))

      val summaries = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/day-summary/$summaryDate")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1PremisesDaySummary::class.java).responseBody.blockFirst()!!

      val capacity = summaries.capacity
      assertThat(capacity.date).isEqualTo(summaryDate)
      assertThat(capacity.totalBedCount).isEqualTo(8)
      assertThat(capacity.availableBedCount).isEqualTo(5)
      assertThat(capacity.bookingCount).isEqualTo(3)
      assertThat(capacity.characteristicAvailability.count()).isEqualTo(6)
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
      assertThat(summary.spaceBookings.size).isEqualTo(1)
      val summaryBooking = summary.spaceBookings[0]
      assertThat(summaryBooking.essentialCharacteristics.map { it.value }).containsExactlyInAnyOrder(
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
      assertThat(summary.spaceBookings).isEmpty()
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

      val spaceBookingsAvailableInPremisesSummary = summaries.spaceBookings
      assertThat(spaceBookingsAvailableInPremisesSummary.size).isEqualTo(2)
      assertThat(spaceBookingsAvailableInPremisesSummary).extracting("id").doesNotContain(excludeSpaceBookingId)

      val capacity = summaries.capacity
      assertThat(capacity.date).isEqualTo(summaryDate)
      assertThat(capacity.totalBedCount).isEqualTo(8)
      assertThat(capacity.availableBedCount).isEqualTo(5)
      assertThat(capacity.bookingCount).isEqualTo(2)

      assertThat(summaries.spaceBookings.map { it.id }).containsExactly(
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

      assertThat(summaries.spaceBookings.size).isEqualTo(3)

      val offender1 = summaries.spaceBookings[0].person
      assertThat((offender1 as FullPersonSummary).name).isEqualTo("firstNameAAA lastNameAAA")
      val offender2 = summaries.spaceBookings[1].person
      assertThat((offender2 as FullPersonSummary).name).isEqualTo("firstNameBBB lastNameBBB")
      val offender3 = summaries.spaceBookings[2].person
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

      assertThat(summaries.spaceBookings.size).isEqualTo(3)

      assertThat(summaries.spaceBookings[0].tier).isEqualTo(tierA)
      assertThat(summaries.spaceBookings[1].tier).isEqualTo(tierB)
      assertThat(summaries.spaceBookings[2].tier).isNull()
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

      assertThat(summaries.spaceBookings.size).isEqualTo(3)

      assertThat(summaries.spaceBookings[0].tier).isNull()
      assertThat(summaries.spaceBookings[1].tier).isEqualTo(tierB)
      assertThat(summaries.spaceBookings[2].tier).isEqualTo(tierA)
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
  inner class GetPremisesStaff : IntegrationTestBase() {

    @Autowired
    lateinit var staffMemberTransformer: StaffMemberTransformer

    @Test
    fun `Request without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/staff")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, mode = EnumSource.Mode.EXCLUDE, names = [ "CAS1_FUTURE_MANAGER", "CAS1_CRU_MEMBER", "CAS1_JANITOR", "CAS1_WORKFLOW_MANAGER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA" ])
    fun `Role without CAS1_VIEW_PREMISES is denied`(role: UserRole) {
      val (_, jwt) = givenAUser(roles = listOf(role))

      webTestClient.get()
        .uri("/cas1/premises/${UUID.randomUUID()}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_CRU_MEMBER" ])
    fun `Returns an empty list if delius team can't be found and raises an alert`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val qCode = "NON_EXISTENT_TEAM_QCODE"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/secure/teams/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(404),
            ),
        )

        val result = webTestClient.get()
          .uri("/cas1/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<StaffMember>()

        assertThat(result).isEmpty()

        mockSentryService.assertErrorMessageRaised("404 returned when finding staff members for qcode 'NON_EXISTENT_TEAM_QCODE'")
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_CRU_MEMBER" ])
    fun `Returns 200 with correct body when user has the CAS1_PREMISES_VIEW permission`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val qCode = "FOUND"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        val staffMembers = listOf(
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
        )

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/approved-premises/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(
                  objectMapper.writeValueAsString(
                    StaffMembersPage(
                      content = staffMembers,
                    ),
                  ),
                ),
            ),
        )

        webTestClient.get()
          .uri("/cas1/premises/${premises.id}/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              staffMembers.map(staffMemberTransformer::transformDomainToApi),
            ),
          )
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = [ "CAS1_FUTURE_MANAGER", "CAS1_CRU_MEMBER" ])
    @Disabled
    fun `Caches response`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val qCode = "FOUND"

        val premises = approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion { givenAProbationRegion() }
          withQCode(qCode)
        }

        val staffMembers = listOf(
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
          ContextStaffMemberFactory().produce(),
        )

        wiremockServer.stubFor(
          WireMock.get(urlEqualTo("/cas1/approved-premises/$qCode/staff"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withStatus(200)
                .withBody(
                  objectMapper.writeValueAsString(
                    StaffMembersPage(
                      content = staffMembers,
                    ),
                  ),
                ),
            ),
        )

        repeat(2) {
          webTestClient.get()
            .uri("/cas1/premises/${premises.id}/staff")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                staffMembers.map(staffMemberTransformer::transformDomainToApi),
              ),
            )
        }

        wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/approved-premises/$qCode/staff")))
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
      )
      val premises2 = givenAnApprovedPremises(
        name = "Premises 2",
        supportsSpaceBookings = true,
        region = givenAProbationRegion(
          apArea = givenAnApArea(name = "Region 2"),
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
        cancellationOccurredAt = LocalDate.now(),
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

      val (_, jwt) = givenAUser(roles = listOf(CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA))

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

          assertThat(actual["row_name"]).isEqualTo(listOf("Region 1 - Premises 1", "Region 2 - Premises 2"))

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
}
