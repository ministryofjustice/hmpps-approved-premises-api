package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchiveBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UnarchivePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3UpdateBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.FutureBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3FutureBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3PremisesTest : Cas3IntegrationTestBase() {
  @Autowired
  private lateinit var premisesRepository: PremisesRepository

  @Autowired
  lateinit var cas3FutureBookingTransformer: Cas3FutureBookingTransformer

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
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStatus = PropertyStatus.archived,
        premisesEndDate = endDate,
      ) { user, jwt, premises ->
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
  }

  @Nested
  inner class GetBedspace {
    @Test
    fun `Get Bedspace by ID returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val roomCharacteristics = getRoomCharacteristics().take(2).toMutableList()
        val roomCharacteristicOne = roomCharacteristics[0]
        val roomCharacteristicTwo = roomCharacteristics[1]

        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          roomCharacteristics = roomCharacteristics,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

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
            startDate = bedspace.createdDate,
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
    }

    @Test
    fun `Get Bedspace by ID returns Not Found with correct body when Premises does not exist`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          bedspaceCount = 1,
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()
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
    }

    @Test
    fun `Get Bedspace by ID returns Not Found with correct body when Bedspace does not exist`() {
      givenATemporaryAccommodationPremisesWithUser(listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          bedspaceCount = 1,
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class GetPremisesBedspaces {
    @Test
    fun `Given a premises with bedspaces when get premises bedspaces then returns OK with correct bedspaces sorted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 5,
          bedspacesStartDates = listOf(
            LocalDate.now().minusMonths(6),
            LocalDate.now().minusMonths(5),
            LocalDate.now().plusDays(5),
            LocalDate.now().minusMonths(4),
            LocalDate.now().minusMonths(9),
          ),
          bedspacesEndDates = listOf(
            null,
            LocalDate.now().plusDays(5),
            null,
            LocalDate.now().minusDays(1),
            LocalDate.now().minusWeeks(1),
          ),
        ) { premises, _, bedspaces ->
          val expectedBedspaces = mutableListOf<Cas3Bedspace>()

          // online bedspaces
          val bedspaceOne = bedspaces.first()
          expectedBedspaces.add(createCas3Bedspace(bedspaceOne, bedspaceOne.room, Cas3BedspaceStatus.online))

          val bedspaceTwo = bedspaces.drop(1).first()
          expectedBedspaces.add(createCas3Bedspace(bedspaceTwo, bedspaceTwo.room, Cas3BedspaceStatus.online))

          // upcoming bedspaces
          val bedspaceThree = bedspaces.drop(2).first()
          expectedBedspaces.add(
            createCas3Bedspace(
              bedspaceThree,
              bedspaceThree.room,
              Cas3BedspaceStatus.upcoming,
              bedspaceThree.startDate,
            ),
          )

          // archived bedspaces
          val bedspaceFour = bedspaces.drop(3).first()
          expectedBedspaces.add(createCas3Bedspace(bedspaceFour, bedspaceFour.room, Cas3BedspaceStatus.archived))

          val bedspaceFive = bedspaces.drop(4).first()
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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwtUser1, premises ->
        givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { user2, jwtUser2 ->

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces")
            .header("Authorization", "Bearer $jwtUser2")
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
          Cas3BedspaceStatus.online -> createBedspaceUnarchiveDomainEvent(bedspace.copy(endDate = date), premisesId, userId, date)
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
  inner class GetPremisesBedspaceTotals {
    @Test
    fun `Get premises bedspace totals returns 200 with correct totals`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 10,
          bedspacesStartDates = listOf(
            // Online
            LocalDate.now().minusDays(10),
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(75),
            // Upcoming
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(3),
            // Archived
            LocalDate.now().minusDays(90),
            LocalDate.now().minusDays(53),
            LocalDate.now().minusDays(25),
          ),
          bedspacesEndDates = listOf(
            // Online
            null,
            null,
            null,
            // Upcoming
            null,
            LocalDate.now().plusDays(31),
            LocalDate.now().plusDays(47),
            null,
            // Archived
            LocalDate.now().minusDays(21),
            LocalDate.now().minusDays(5),
            LocalDate.now().minusDays(2),
          ),
        ) { premises, _, _ ->

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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(90),
          LocalDate.now().minusDays(53),
          LocalDate.now().minusDays(25),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(21),
          LocalDate.now().minusDays(5),
          LocalDate.now().minusDays(2),
        ),
      ) { user, jwt, premises, _, _ ->

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
          .jsonPath("$.totalArchivedBedspaces").isEqualTo(3)
      }
    }
  }

  @Nested
  inner class CreatePremises {
    @Test
    fun `Create new premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(user.probationRegion)
        }

        val characteristics = createCharacteristics(5, "premises").sortedBy { it.id }

        val characteristicIds = characteristics.map { it.id }

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
        val anotherProbationRegion = probationRegionEntityFactory.produceAndPersist()
        val pdu = probationDeliveryUnitFactory.produceAndPersist {
          withProbationRegion(anotherProbationRegion)
        }

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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Update premises returns 200 OK with correct body`(isNewTurnaroundWorkingDaysField: Boolean) {
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
          turnaroundWorkingDayCount = if (isNewTurnaroundWorkingDaysField) null else 3,
          turnaroundWorkingDays = if (isNewTurnaroundWorkingDaysField) 3 else null,
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Update premises returns 200 OK with correct body when the reference hasn't been changed`(isNewTurnaroundWorkingDaysField: Boolean) {
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
          turnaroundWorkingDayCount = if (isNewTurnaroundWorkingDaysField) null else 3,
          turnaroundWorkingDays = if (isNewTurnaroundWorkingDaysField) 3 else null,
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Update premises returns 403 Forbidden when user access is not allowed as they are out of region`(isNewTurnaroundWorkingDaysField: Boolean) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenATemporaryAccommodationPremises { premises ->
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
            turnaroundWorkingDayCount = if (isNewTurnaroundWorkingDaysField) null else 3,
            turnaroundWorkingDays = if (isNewTurnaroundWorkingDaysField) 3 else null,
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
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Update premises returns 404 when premises to update is not found`(isNewTurnaroundWorkingDaysField: Boolean) {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
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
          probationRegionId = user.probationRegion.id,
          probationDeliveryUnitId = probationDeliveryUnit.id,
          localAuthorityAreaId = localAuthorityArea.id,
          characteristicIds = emptyList(),
          turnaroundWorkingDayCount = if (isNewTurnaroundWorkingDaysField) null else 3,
          turnaroundWorkingDays = if (isNewTurnaroundWorkingDaysField) 3 else null,
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
  inner class CreateBedspace {
    @Test
    fun `Create new bedspace for Premises returns 201 Created with correct body`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val characteristics = createCharacteristics(5, "room")
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
        val characteristics = createCharacteristics(5, "room")
        val characteristicIds = characteristics.map { it.id }

        val premisesArchiveDate = LocalDate.now().plusDays(10)
        createPremisesArchiveDomainEvent(premises, user, premisesArchiveDate)

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
          .jsonPath("startDate").isEqualTo(bedspaceStartDate)
          .jsonPath("notes").isEqualTo(newBedspace.notes)
          .jsonPath("characteristics[*].id").isEqualTo(characteristicIds.map { it.toString() })
          .jsonPath("characteristics[*].modelScope").isEqualTo(MutableList(5) { "room" })
          .jsonPath("characteristics[*].serviceScope").isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
          .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })

        // verify premises is unarchived
        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.endDate).isNull()
      }
    }

    @Test
    fun `When a new bedspace is created with no notes then it defaults to empty`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val characteristicId = createCharacteristics(1, "room", ServiceName.approvedPremises.value).first().id

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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val characteristicId = getPremisesCharacteristics().first().id

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
        givenATemporaryAccommodationPremises { premises ->
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
  }

  @Nested
  inner class UpdateBedspace {
    @Test
    fun `When updating a bedspace returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(360)),
          bedspacesEndDates = listOf(null),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()
          val characteristics = createCharacteristics(5, "room")
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
            .jsonPath("characteristics[*].serviceScope")
            .isEqualTo(MutableList(5) { ServiceName.temporaryAccommodation.value })
            .jsonPath("characteristics[*].name").isEqualTo(characteristics.map { it.name })
        }
      }
    }

    @Test
    fun `When updating a bedspace without notes it will default to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(360)),
          bedspacesEndDates = listOf(null),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()

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
    }

    @Test
    fun `When updating a bedspace with empty reference returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(90)),
          bedspacesEndDates = listOf(LocalDate.now().minusDays(30)),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()

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
    }

    @Test
    fun `When updating a bedspace with an unknown characteristic returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(90)),
          bedspacesEndDates = listOf(null),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()

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
    }

    @Test
    fun `When updating a bedspace with a characteristic that has an incorrect service scope returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(90)),
          bedspacesEndDates = listOf(null),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()
          val characteristicId = createCharacteristics(1, "room", ServiceName.approvedPremises.value).first().id

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
    }

    @Test
    fun `When updating a bedspace with a characteristic that has an incorrect model scope returns Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(60)),
          bedspacesEndDates = listOf(null),
        ) { premises, _, bedspaces ->
          val bedspace = bedspaces.first()

          val characteristicId = getPremisesCharacteristics().first().id

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
  }

  @Nested
  inner class CanArchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-26T00:00:00Z"))
    }

    @Test
    fun `Can archive premises returns 200 when no blocking bookings or voids exist`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt, premises ->
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

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3PremisesTest#getCanArchivePremisesBookingsByStatusCases")
    fun `Can archive premises returns 200 when bookings have departure date after 3 months`(args: Pair<LocalDate, BookingStatus>) {
      val (departureDate, status) = args
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now().minusDays(1),
            departureDate = departureDate,
            status = status,
          )

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.items").isArray
            .jsonPath("$.items.length()").isEqualTo(1)
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.room.name)
            .jsonPath("$.items[0].date").isEqualTo(departureDate)
        }
      }
    }

    @Test
    fun `Can archive premises returns 200 when voids have end dates after 3 months`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val futureEndDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
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
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.room.name)
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(4)
          val latestBlockingDate = futureVoidEndDate.plusWeeks(1)

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(1),
            departureDate = latestBlockingDate,
          )

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
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
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.room.name)
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val nearFutureDepartureDate = LocalDate.now(clock).plusMonths(2) // Within 3 months

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(1),
            departureDate = nearFutureDepartureDate,
          )

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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val nearFutureEndDate = LocalDate.now(clock).plusMonths(2)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
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
        ) { premises, rooms, bedspaces ->
          val bedspace1 = bedspaces[0]
          val bedspace2 = bedspaces[1]
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(5)

          createBooking(
            premises = premises,
            bedspace = bedspace1,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = futureDepartureDate,
          )

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace2)
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
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace1.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace1.room.name)
            .jsonPath("$.items[0].date").isEqualTo(futureDepartureDate)
            .jsonPath("$.items[1].entityId").isEqualTo(bedspace2.id.toString())
            .jsonPath("$.items[1].entityReference").isEqualTo(bedspace2.room.name)
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = futureDepartureDate,
          )

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
    fun `Can archive premises returns 200 when void is cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val futureDepartureDate = LocalDate.now(clock).plusMonths(4)

          val voidBedspace = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(futureDepartureDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
            withVoidBedspace(voidBedspace)
            withNotes(randomStringMultiCaseWithNumbers(50))
            withCreatedAt(OffsetDateTime.now())
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
    fun `Can archive premises returns 200 with edge case exactly 3 months minus one day`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val exactlyThreeMonthsDate = LocalDate.now(clock).plusMonths(3).minusDays(1)

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = exactlyThreeMonthsDate,
          )

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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val justUnder3Months = LocalDate.now(clock).plusMonths(3).minusDays(2)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = justUnder3Months,
          )

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
            .jsonPath("$.items[0].entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.items[0].entityReference").isEqualTo(bedspace.room.name)
            .jsonPath("$.items[0].date").isEqualTo(justUnder3Months.plusDays(3))
        }
      }
    }
  }

  @Nested
  @Deprecated("Superseded by Cas3v2PremisesArchiveTest.")
  inner class ArchivePremises {
    @Test
    fun `Given archive a premises when successfully passed all validations then returns 200 OK`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedspacesEndDates = listOf(
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

          val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(premisesDomainEvents).hasSize(1)

          val updatedBedspaces = bedRepository.findByRoomPremisesId(updatedPremises.id)
          assertThat(updatedBedspaces).hasSize(3)
          assertThat(updatedBedspaces[0].id).isEqualTo(bedspaceTwo.id)
          assertThat(updatedBedspaces[0].endDate).isEqualTo(bedspaceTwo.endDate)
          assertThat(updatedBedspaces[1].id).isEqualTo(bedspaceOne.id)
          assertThat(updatedBedspaces[1].endDate).isEqualTo(LocalDate.now())
          assertThat(updatedBedspaces[2].id).isEqualTo(bedspaceThree.id)
          assertThat(updatedBedspaces[2].endDate).isEqualTo(LocalDate.now())

          val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaceOne.id)
          assertThat(bedspaceOneDomainEvents).hasSize(1)
          assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)

          val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaceThree.id)
          assertThat(bedspaceThreeDomainEvents).hasSize(1)
          assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
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

        val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_ARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)
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
          bedspacesStartDates = listOf(LocalDate.now().minusDays(100), LocalDate.now().plusDays(5)),
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
          bedspacesStartDates = listOf(
            LocalDate.now().minusDays(100),
            LocalDate.now().minusDays(75),
            LocalDate.now().minusDays(30),
          ),
          bedspaceCount = 3,
        ) { premises, rooms, bedspaces ->
          val premisesArchiveDate = LocalDate.now().plusDays(5)

          val bedspaceOne = bedspaces.first()
          val bookingDepartureDate = premisesArchiveDate.plusDays(10)

          createBooking(
            premises = premises,
            bedspace = bedspaceOne,
            arrivalDate = premisesArchiveDate.minusDays(2),
            departureDate = bookingDepartureDate,
          )

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
          createBooking(
            premises = premises,
            bedspace = bedspaceThree,
            arrivalDate = premisesArchiveDate.minusDays(35),
            departureDate = premisesArchiveDate.plusDays(3),
          )

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
  inner class CanArchiveBedspace {
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .isEmpty()
        }
      }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3PremisesTest#getCanArchiveBedspaceBookingsByStatusCases")
    fun `Can archive bedspace returns 200 with Cas3ValidationResult when a provisional booking departure date is more than 3 months from today's date`(args: Pair<LocalDate, BookingStatus>) {
      val (departureDate, status) = args
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now().plusDays(1),
            departureDate = departureDate,
            status = status,
          )

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.entityId").isEqualTo(bedspace.id.toString())
            .jsonPath("$.entityReference").isEqualTo(bedspace.room.name)
            .jsonPath("$.date").isEqualTo(departureDate.toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 when a booking is cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = LocalDate.now(clock).plusMonths(4),
          )

          cancellationEntityFactory.produceAndPersist {
            withBooking(booking)
            withReason(cancellationReasonEntityFactory.produceAndPersist())
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
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
    fun `Can archive bedspace returns 200 when void is cancelled`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          val voidBedspace = cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
            withPremises(premises)
            withStartDate(LocalDate.now(clock).plusDays(1))
            withEndDate(LocalDate.now(clock).plusMonths(4))
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
          }

          cas3VoidBedspaceCancellationEntityFactory.produceAndPersist {
            withVoidBedspace(voidBedspace)
            withNotes(randomStringMultiCaseWithNumbers(50))
            withCreatedAt(OffsetDateTime.now())
          }

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val futureVoidEndDate = LocalDate.now(clock).plusMonths(4)

          cas3VoidBedspaceEntityFactory.produceAndPersist {
            withBed(bedspace)
            withStartDate(LocalDate.now(clock).plusDays(10))
            withEndDate(futureVoidEndDate)
            withYieldedReason { cas3VoidBedspaceReasonEntityFactory.produceAndPersist() }
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
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val justUnder3Months = LocalDate.now(clock).plusMonths(3)

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).plusDays(1),
            departureDate = justUnder3Months,
          )

          cas3TurnaroundFactory.produceAndPersist {
            withWorkingDayCount(2)
            withBooking(booking)
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
            .jsonPath("$.date").isEqualTo(justUnder3Months.plusDays(2).toString())
        }
      }
    }

    @Test
    fun `Can archive bedspace returns 200 when booking departure date is 3 months minus 1 day from today's date`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = userEntity.probationRegion,
          bedspaceCount = 1,
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val exactlyThreeMonthsMinusOneDay = LocalDate.now(clock).plusMonths(3).minusDays(1)

          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(5),
            departureDate = exactlyThreeMonthsMinusOneDay,
          )

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
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
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(bedspaceCount = 1) { premises, rooms, bedspaces ->
          givenATemporaryAccommodationPremisesWithRoomsAndBeds(bedspaceCount = 1) { premisesTwo, rooms, bedspacesPremisesTwo ->
            val bedspace = bedspacesPremisesTwo.first()

            webTestClient.get()
              .uri("/cas3/premises/${premisesTwo.id}/bedspaces/${bedspace.id}/can-archive")
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremises { premises ->
          val bedspace =
            createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(360), endDate = null)

          webTestClient.get()
            .uri("/cas3/premises/${premises.id}/bedspaces/${bedspace.id}/can-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class ArchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-07-03T10:15:30Z"))
    }

    @Test
    fun `When archive a bedspace returns OK with correct body when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 3,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
            // upcoming bedspace
            LocalDate.now(clock).plusDays(4),
          ),
          bedspacesEndDates = listOf(
            null,
            null,
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspaceOne = bedspaces.first()

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
          ),
          bedspacesEndDates = listOf(
            null,
            LocalDate.now(clock).minusDays(2),
          ),
        ) { premises, rooms, bedspaces ->
          val bedspaceOne = bedspaces.first()

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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

          val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
          assertThat(updatedPremises).isNotNull()
          assertThat(updatedPremises?.endDate).isEqualTo(archiveBedspace.endDate)
          assertThat(updatedPremises?.status).isEqualTo(PropertyStatus.archived)

          val updatedBedspace = bedRepository.findByIdOrNull(bedspaceOne.id)
          assertThat(updatedBedspace).isNotNull()
          assertThat(updatedBedspace?.endDate).isEqualTo(updatedBedspace?.endDate)

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(2)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
          assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive the last online bedspace returns OK and archives the premises with the latest bedspae end date when given valid data`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val latestBedspaceArchiveDate = LocalDate.now(clock).plusDays(35)
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
            LocalDate.now(clock).minusDays(180),
          ),
          bedspacesEndDates = listOf(
            null,
            latestBedspaceArchiveDate,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspaceOne = bedspaces.first()

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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

          val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
          assertThat(updatedPremises).isNotNull()
          assertThat(updatedPremises?.endDate).isEqualTo(latestBedspaceArchiveDate)
          assertThat(updatedPremises?.status).isEqualTo(PropertyStatus.archived)

          val updatedBedspace = bedRepository.findByIdOrNull(bedspaceOne.id)
          assertThat(updatedBedspace).isNotNull()
          assertThat(updatedBedspace?.endDate).isEqualTo(updatedBedspace?.endDate)

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(2)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(bedspaceOne.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
          assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_ARCHIVED)
          assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `When archive a bedspace for a Premises that not exist returns 404 Not Found`() {
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt, premises ->
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(360), endDate = null)
        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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
      givenATemporaryAccommodationPremisesWithUser(roles = listOf(UserRole.CAS3_ASSESSOR), premisesStartDate = LocalDate.now(clock).minusDays(3)) { user, jwt, premises ->
        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(2), endDate = null)

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).minusDays(5))

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
        val previousBedspaceArchiveDate = LocalDate.now(clock).minusDays(3)

        val bedspace = createBedspaceInPremises(premises, startDate = LocalDate.now(clock).minusDays(300), endDate = null)

        createBedspaceArchiveDomainEvent(bedspace.id, premises.id, user.id, null, previousBedspaceArchiveDate)

        val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).minusDays(3))

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
    fun `When archive a bedspace with an active booking after the future archiving date returns 400 Bad Request`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          val bookingDepartureDate = LocalDate.now(clock).plusDays(10)
          createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(20),
            departureDate = bookingDepartureDate,
          )

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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
    }

    @Test
    fun `When archive a bedspace with a void end date after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          val bedspaceArchivingDate = LocalDate.now(clock).plusDays(10)
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
    }

    @Test
    fun `When archive a bedspace with a booking turnaround after the bedspace archive date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

          val bedspaceArchivingDate = LocalDate.now(clock).plusDays(10)

          val booking = createBooking(
            premises = premises,
            bedspace = bedspace,
            arrivalDate = LocalDate.now(clock).minusDays(20),
            departureDate = bedspaceArchivingDate.minusDays(1),
          )

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
    }

    @Test
    fun `When archive a bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(360),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()

          val archiveBedspace = Cas3ArchiveBedspace(LocalDate.now(clock).plusDays(5))

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
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(74),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(1),
          LocalDate.now().minusDays(5),
          LocalDate.now().minusDays(19),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(1))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val updatedBedspaces = bedRepository.findAll()
        assertThat(updatedBedspaces).hasSize(3)
        updatedBedspaces.forEach { bedspace ->
          assertThat(bedspace.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
          assertThat(bedspace.endDate).isNull()
        }

        val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)

        val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[0].id)
        assertThat(bedspaceOneDomainEvents).hasSize(1)
        assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceTwoDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[1].id)
        assertThat(bedspaceTwoDomainEvents).hasSize(1)
        assertThat(bedspaceTwoDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[2].id)
        assertThat(bedspaceThreeDomainEvents).hasSize(1)
        assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
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
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(130),
          LocalDate.now().minusDays(20),
          LocalDate.now().minusDays(80),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(11),
          LocalDate.now().minusDays(21),
          LocalDate.now().minusDays(1),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(5))
        val bedspaceThree = bedspaces.drop(2).first()

        // previous bedspace three
        val duplicatedRoom = roomEntityFactory.produceAndPersist {
          withPremises(premises)
          withName(bedspaceThree.room.name)
          withNotes(randomStringLowerCase(100))
        }

        val duplicatedBedspaceThree = bedEntityFactory.produceAndPersist {
          withStartDate(LocalDate.now().minusDays(120))
          withEndDate(LocalDate.now().minusDays(90))
          withRoom(duplicatedRoom)
        }

        duplicatedBedspaceThree.createdAt = OffsetDateTime.now().minusDays(120)
        bedRepository.save(duplicatedBedspaceThree)

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("id").isEqualTo(premises.id.toString())
          .jsonPath("status").isEqualTo("archived")

        val updatedPremises = temporaryAccommodationPremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(PropertyStatus.active)
        assertThat(updatedPremises.startDate).isEqualTo(cas3UnarchivePremises.restartDate)
        assertThat(updatedPremises.endDate).isNull()

        val premisesDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesDomainEvents).hasSize(1)

        val allBedspaces = bedRepository.findAll()
        val updateBedspace = allBedspaces.filter { it.startDate == cas3UnarchivePremises.restartDate }
        assertThat(updateBedspace).hasSize(3)
        assertThat(updateBedspace.map { it.id }).contains(bedspaceThree.id)
        assertThat(updateBedspace.map { it.id }).doesNotContain(duplicatedBedspaceThree.id)

        val bedspaceOneDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[0].id)
        assertThat(bedspaceOneDomainEvents).hasSize(1)
        assertThat(bedspaceOneDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceTwoDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[1].id)
        assertThat(bedspaceTwoDomainEvents).hasSize(1)
        assertThat(bedspaceTwoDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val bedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(bedspaces[2].id)
        assertThat(bedspaceThreeDomainEvents).hasSize(1)
        assertThat(bedspaceThreeDomainEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)

        val duplicatedBedspaceThreeDomainEvents = domainEventRepository.findByCas3BedspaceId(duplicatedBedspaceThree.id)
        assertThat(duplicatedBedspaceThreeDomainEvents).hasSize(0)
      }
    }

    @Test
    fun `Unarchive premises returns 400 when restart date is too far in the past`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(10),
        premisesStatus = PropertyStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().minusDays(8))
        val restartDate = LocalDate.now()

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
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
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = LocalDate.now().minusDays(1),
        premisesStatus = PropertyStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(9))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
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
      val lastArchiveEndDate = LocalDate.now().minusDays(3)
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(30),
        premisesEndDate = lastArchiveEndDate,
        premisesStatus = PropertyStatus.archived,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().minusDays(6))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
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
        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(4))

        webTestClient.post()
          .uri("/cas3/premises/$nonExistentPremisesId/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
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
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStatus = PropertyStatus.active,
      ) { _, jwt, premises ->

        val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(3))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchivePremises)
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
        givenATemporaryAccommodationPremises(
          startDate = LocalDate.now().minusDays(30),
          endDate = LocalDate.now().minusDays(1),
          status = PropertyStatus.archived,
        ) { premises ->

          val cas3UnarchivePremises = Cas3UnarchivePremises(LocalDate.now().plusDays(2))

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/unarchive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(cas3UnarchivePremises)
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class UnarchiveBedspace {
    @Test
    fun `Unarchive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            LocalDate.now(clock).minusDays(1),
          ),
        ) { premises, rooms, bedspaces ->
          val archivedBedspace = bedspaces.first()

          val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(1))

          webTestClient.post()
            .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(cas3UnarchiveBedspace)
            .exchange()
            .expectStatus()
            .isOk

          // Verify the bedspace was updated
          val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
          assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
          assertThat(updatedBedspace.endDate).isNull()

          val allEvents = domainEventRepository.findAll()
          assertThat(allEvents).hasSize(1)
          assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
          assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
          assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        }
      }
    }

    @Test
    fun `Unarchive bedspace when premises is archived returns 200 OK and unarchive premises and bedspace successfully`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now().minusDays(30),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(40),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(1))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(2)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
        assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
        assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
        assertThat(allEvents[1].type).isEqualTo(DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(allEvents[1].cas3PremisesId).isEqualTo(premises.id)
      }
    }

    @Test
    fun `Unarchive bedspace when premises is online returns 200 OK and unarchive bedspace successfully`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(40),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(5))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
          .exchange()
          .expectStatus()
          .isOk

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(archivedBedspace.id).get()
        assertThat(updatedBedspace.startDate).isEqualTo(cas3UnarchiveBedspace.restartDate)
        assertThat(updatedBedspace.endDate).isNull()

        // Verify the premises was not updated
        val updatedPremises = temporaryAccommodationPremisesRepository.findByIdOrNull(premises.id)
        assertThat(updatedPremises).isNotNull()
        assertThat(updatedPremises?.startDate).isEqualTo(premises.startDate)
        assertThat(updatedPremises?.endDate).isNull()

        val allEvents = domainEventRepository.findAll()
        assertThat(allEvents).hasSize(1)
        assertThat(allEvents[0].type).isEqualTo(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
        assertThat(allEvents[0].cas3BedspaceId).isEqualTo(archivedBedspace.id)
        assertThat(allEvents[0].cas3PremisesId).isEqualTo(premises.id)
      }
    }

    @Test
    fun `Unarchive bedspace returns 400 when restart date is too far in the past`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(10),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().minusDays(8))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(1),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(8))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now().minusDays(5),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().minusDays(7))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
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
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { _, jwt, premises ->

        val nonExistentBedspaceId = UUID.randomUUID()
        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(3))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/$nonExistentBedspaceId/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val onlineBedspace = bedspaces.first()

        val cas3UnarchiveBedspace = Cas3UnarchiveBedspace(LocalDate.now().plusDays(2))

        webTestClient.post()
          .uri("/cas3/premises/${premises.id}/bedspaces/${onlineBedspace.id}/unarchive")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(cas3UnarchiveBedspace)
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
        givenATemporaryAccommodationPremises { premises ->
          val archivedBedspace = createBedspaceInPremises(
            premises,
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().minusDays(1),
          )

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
  }

  @Nested
  inner class CancelScheduledArchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-07-16T21:33:04Z"))
    }

    @Test
    fun `Cancel scheduled archive premises returns 200 OK when successful`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesEndDate = LocalDate.now(clock).plusDays(10),
        premisesStatus = PropertyStatus.archived,
        bedspaceCount = 3,
        bedspaceEndDates = listOf(
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
          LocalDate.now(clock).plusDays(10),
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val domainEventTransactionId = UUID.randomUUID()

        val premisesArchiveDate = premises.endDate!!
        val premisesArchiveDomainEvent = createPremisesArchiveDomainEvent(premises, user, premisesArchiveDate, transactionId = domainEventTransactionId)

        val bedspaceOne = bedspaces.first()
        val bedspaceOneArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceOne.id, premises.id, user.id, null, premisesArchiveDate, transactionId = domainEventTransactionId)
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceTwoCurrentEndDate = bedspaceTwo.endDate ?: premisesArchiveDate.plusDays(12)
        val bedspaceTwoArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, bedspaceTwoCurrentEndDate, premisesArchiveDate, transactionId = domainEventTransactionId)
        val bedspaceThree = bedspaces.drop(2).first()
        val bedspaceThreeArchiveDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, premisesArchiveDate, transactionId = domainEventTransactionId)

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
        premisesEndDate = LocalDate.now(clock),
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
        premisesEndDate = LocalDate.now(clock).minusDays(1),
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
  inner class CancelScheduledArchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-27T15:21:34Z"))
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 200 OK when successful`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val scheduledArchiveBedspaceDate = LocalDate.now().plusDays(1)

        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          region = user.probationRegion,
          bedspaceCount = 2,
          bedspacesStartDates = listOf(LocalDate.now().minusDays(30), LocalDate.now().minusDays(60)),
          bedspacesEndDates = listOf(scheduledArchiveBedspaceDate),
        ) { premises, rooms, bedspaces ->
          val bedspace = bedspaces.first()
          val previousBedspaceEndDate = LocalDate.now().minusDays(10)
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
    }

    @Test
    fun `When cancel scheduled archive bedspace in a premises that is scheduled to be archived returns Success and cancels the scheduled archiving of the premises and all associated bedspaces`() {
      val archivedPremisesDate = LocalDate.now().plusWeeks(1)
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = LocalDate.now().minusDays(180),
        premisesEndDate = archivedPremisesDate,
        premisesStatus = PropertyStatus.active,
        bedspaceCount = 3,
        bedspaceStartDates = listOf(
          LocalDate.now().minusDays(180),
          LocalDate.now().minusDays(90),
          LocalDate.now().minusDays(30),
        ),
        bedspaceEndDates = listOf(
          archivedPremisesDate,
          LocalDate.now().minusDays(13),
          archivedPremisesDate,
        ),
      ) { user, jwt, premises, rooms, bedspaces ->
        val bedspaceOne = bedspaces.first()
        val bedspaceTwo = bedspaces.drop(1).first()
        val bedspaceThree = bedspaces.drop(2).first()

        val previousBedspaceOneEndDate = LocalDate.now().minusDays(10)
        val domainEventTransactionId = UUID.randomUUID()

        val archivePremisesDomainEvent = createPremisesArchiveDomainEvent(premises, user, archivedPremisesDate, transactionId = domainEventTransactionId)
        val archiveBedspaceOneDomainEvent = createBedspaceArchiveDomainEvent(bedspaceOne.id, premises.id, user.id, previousBedspaceOneEndDate, archivedPremisesDate, transactionId = domainEventTransactionId)
        val archiveBedspaceTwoDomainEvent = createBedspaceArchiveDomainEvent(bedspaceTwo.id, premises.id, user.id, null, bedspaceTwo.endDate!!, transactionId = UUID.randomUUID())
        val archiveBedspaceThreeDomainEvent = createBedspaceArchiveDomainEvent(bedspaceThree.id, premises.id, user.id, null, archivedPremisesDate, transactionId = domainEventTransactionId)

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
        assertThatBedspaceArchiveCancelled(bedspaceOne.id, archiveBedspaceOneDomainEvent.id, previousBedspaceOneEndDate)
        assertThatBedspaceArchiveCancelled(bedspaceThree.id, archiveBedspaceThreeDomainEvent.id, null)

        // check that bedspace not part of a scheduled archive not updated
        val notUpdatedBedspace = bedRepository.findById(bedspaceTwo.id).get()
        assertThat(notUpdatedBedspace.endDate).isEqualTo(bedspaceTwo.endDate)

        val notUpdatedBedspaceArchiveDomainEvent =
          domainEventRepository.findByIdOrNull(archiveBedspaceTwoDomainEvent.id)
        assertThat(notUpdatedBedspaceArchiveDomainEvent).isNotNull()
        assertThat(notUpdatedBedspaceArchiveDomainEvent?.cas3CancelledAt).isNull()
      }
    }

    @Test
    fun `Cancel scheduled archive bedspace returns 403 when user does not have permission to manage premises without CAS3_ASSESOR role`() {
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_REFERRER),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(30),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now(clock).plusDays(1),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val scheduledToArchivedBedspace = bedspaces.first()

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
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { _, jwt, premises ->

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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val onlineBedspace = bedspaces.first()

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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          LocalDate.now(clock).minusDays(1),
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val archivedBedspace = bedspaces.first()

        webTestClient.put()
          .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/cancel-archive")
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
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            LocalDate.now(clock).minusDays(1),
          ),
        ) { premises, rooms, bedspaces ->
          val archivedBedspace = bedspaces.first()

          webTestClient.put()
            .uri("/cas3/premises/${premises.id}/bedspaces/${archivedBedspace.id}/cancel-archive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
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
  inner class CancelScheduledUnarchivePremises {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-08-27T15:21:34Z"))
    }

    @Test
    fun `Cancel scheduled unarchive premises returns 200 OK when successful`() {
      val previousStartDate = LocalDate.now(clock).minusDays(60)
      val previousEndDate = previousStartDate.plusDays(30)
      val newStartDate = LocalDate.now(clock).plusDays(5)
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        premisesStartDate = previousStartDate,
        premisesEndDate = previousEndDate,
        premisesStatus = PropertyStatus.archived,
      ) { userEntity, jwt, premises ->
        premises.createdAt = OffsetDateTime.now(clock).minusDays(60)
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
        assertThat(updatedPremises.startDate).isEqualTo(premises.createdAt.toLocalDate())
        assertThat(updatedPremises.endDate).isEqualTo(previousEndDate)

        val updatedPremisesUnarchiveDomainEvent =
          domainEventRepository.findByIdOrNull(lastPremisesUnarchiveDomainEvent.id)
        assertThat(updatedPremisesUnarchiveDomainEvent).isNotNull()
        assertThat(updatedPremisesUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))

        // Verify that bedspace was updated
        val updatedBed = bedRepository.findById(bedspace.id).get()
        assertThat(updatedBed.startDate).isEqualTo(previousStartDate)
        assertThat(updatedBed.endDate).isEqualTo(previousEndDate)

        val updatedBedspaceUnarchiveDomainEvent =
          domainEventRepository.findByIdOrNull(lastBedspaceUnarchiveDomainEvent.id)
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
        givenATemporaryAccommodationPremises { premises ->

          webTestClient.put()
            .uri("/cas3/premises/${premises.id}/cancel-unarchive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
        }
      }
    }
  }

  @Nested
  inner class CancelScheduledUnarchiveBedspace {
    @BeforeEach
    fun setup() {
      clock.setNow(Instant.parse("2025-09-03T11:34:09Z"))
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 200 OK when successful`() {
      givenATemporaryAccommodationPremisesWithUser(
        roles = listOf(UserRole.CAS3_ASSESSOR),
      ) { userEntity, jwt, premises ->
        val originalStartDate = LocalDate.now(clock).minusDays(10)
        val originalEndDate = LocalDate.now(clock).minusDays(3)

        val scheduledBedspaceToUnarchived = createBedspaceInPremises(premises, originalStartDate, originalEndDate)

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
          .jsonPath("startDate").isEqualTo(scheduledBedspaceToUnarchived.createdDate)
          .jsonPath("endDate").isEqualTo(originalEndDate)

        // Verify the bedspace was updated
        val updatedBedspace = bedRepository.findById(scheduledBedspaceToUnarchived.id).get()
        assertThat(updatedBedspace.createdDate).isEqualTo(scheduledBedspaceToUnarchived.createdDate)
        assertThat(updatedBedspace.startDate).isEqualTo(originalStartDate)
        assertThat(updatedBedspace.endDate).isEqualTo(originalEndDate)

        val updatedBedspaceUnarchiveDomainEvent = domainEventRepository.findByIdOrNull(lastBedspaceUnarchiveDomainEvent.id)
        assertThat(updatedBedspaceUnarchiveDomainEvent).isNotNull()
        assertThat(updatedBedspaceUnarchiveDomainEvent?.cas3CancelledAt).isEqualTo(OffsetDateTime.now(clock))
      }
    }

    @Test
    fun `Cancel scheduled unarchive bedspace returns 400 when it has a field validation error (startDate is today)`() {
      val startDate = LocalDate.now(clock)

      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_ASSESSOR),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          startDate,
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val scheduledToUnarchivedBedspace = bedspaces.first()

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
      givenATemporaryAccommodationPremisesComplete(
        roles = listOf(UserRole.CAS3_REFERRER),
        bedspaceCount = 1,
        bedspaceStartDates = listOf(
          LocalDate.now(clock).minusDays(10),
        ),
        bedspaceEndDates = listOf(
          null,
        ),
      ) { _, jwt, premises, _, bedspaces ->
        val scheduledToUnarchivedBedspace = bedspaces.first()

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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        givenATemporaryAccommodationPremisesWithRoomsAndBeds(
          bedspaceCount = 1,
          bedspacesStartDates = listOf(
            LocalDate.now(clock).minusDays(30),
          ),
          bedspacesEndDates = listOf(
            null,
          ),
        ) { premises, rooms, bedspaces ->
          val scheduledToUnarchivedBedspace = bedspaces.first()

          webTestClient.put()
            .uri("/cas3/premises/${premises.id}/bedspaces/${scheduledToUnarchivedBedspace.id}/cancel-unarchive")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isForbidden
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

  private fun createCharacteristics(count: Int, modelScope: String, serviceName: String = ServiceName.temporaryAccommodation.value) = characteristicEntityFactory.produceAndPersistMultiple(count) {
    withModelScope(modelScope)
    withServiceScope(serviceName)
    withName(randomStringMultiCaseWithNumbers(10))
  }

  private fun createBooking(
    premises: TemporaryAccommodationPremisesEntity,
    bedspace: BedEntity,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    status: BookingStatus = BookingStatus.provisional,
  ) = bookingEntityFactory.produceAndPersist {
    withServiceName(ServiceName.temporaryAccommodation)
    withPremises(premises)
    withBed(bedspace)
    withCrn(randomStringMultiCaseWithNumbers(6))
    withArrivalDate(arrivalDate)
    withDepartureDate(departureDate)
    withStatus(status)
  }

  private companion object {
    @JvmStatic
    fun getArchivedPremisesByIdCases() = listOf(
      Pair(LocalDate.now().minusDays(1), Cas3PremisesStatus.archived),
      Pair(LocalDate.now().plusDays(5), Cas3PremisesStatus.online),
    )

    @JvmStatic
    fun getCanArchivePremisesBookingsByStatusCases() = listOf(
      Pair(LocalDate.now().plusMonths(4), BookingStatus.provisional),
      Pair(LocalDate.now().plusMonths(4), BookingStatus.arrived),
      Pair(LocalDate.now().plusMonths(5), BookingStatus.confirmed),
    )

    @JvmStatic
    fun getCanArchiveBedspaceBookingsByStatusCases() = listOf(
      Pair(LocalDate.now().plusMonths(4), BookingStatus.provisional),
      Pair(LocalDate.now().plusMonths(5), BookingStatus.arrived),
      Pair(LocalDate.now().plusMonths(6), BookingStatus.confirmed),
    )
  }
}
