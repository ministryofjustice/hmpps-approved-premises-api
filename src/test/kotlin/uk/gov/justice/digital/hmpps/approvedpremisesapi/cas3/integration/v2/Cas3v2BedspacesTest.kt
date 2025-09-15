package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.v2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens.givenACas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Bedspaces
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NewBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3v2BedspacesTest : Cas3IntegrationTestBase() {

  @Nested
  inner class CreateBedspace {
    @Test
    fun `Create new bedspace for Premises returns 201 Created with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val bedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(5)
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = bedspaceCharacteristics.map { it.id },
          notes = randomStringLowerCase(100),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newBedspace.reference)
          .jsonPath("startDate").isEqualTo(newBedspace.startDate.toString())
          .jsonPath("notes").isEqualTo(newBedspace.notes.toString())
          .jsonPath("bedspaceCharacteristics[*].id").isEqualTo(bedspaceCharacteristics.map { it.id.toString() })
          .jsonPath("bedspaceCharacteristics[*].name").isEqualTo(bedspaceCharacteristics.map { it.name })
      }
    }

    @Test
    fun `Create new bedspace in a scheduled to archive premises returns 201 Created with correct body and unarchive the premises`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.archived,
          endDate = LocalDate.now().plusDays(3),
        )
        val bedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(5)
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now().minusDays(2),
          characteristicIds = bedspaceCharacteristics.map { it.id },
          notes = randomStringLowerCase(100),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody()
          .jsonPath("reference").isEqualTo(newBedspace.reference)
          .jsonPath("startDate").isEqualTo(LocalDate.now())
          .jsonPath("notes").isEqualTo(newBedspace.notes.toString())
          .jsonPath("bedspaceCharacteristics[*].id").isEqualTo(bedspaceCharacteristics.map { it.id.toString() })
          .jsonPath("bedspaceCharacteristics[*].name").isEqualTo(bedspaceCharacteristics.map { it.name })

        // verify premises is unarchived
        val updatedPremises = cas3PremisesRepository.findById(premises.id).get()
        assertThat(updatedPremises.status).isEqualTo(Cas3PremisesStatus.online)
        assertThat(updatedPremises.startDate).isEqualTo(newBedspace.startDate)
        assertThat(updatedPremises.endDate).isNull()

        val premisesUnarchiveDomainEvents = domainEventRepository.findByCas3PremisesIdAndType(premises.id, DomainEventType.CAS3_PREMISES_UNARCHIVED)
        assertThat(premisesUnarchiveDomainEvents).isNotNull()
        assertThat(premisesUnarchiveDomainEvents.size).isEqualTo(1)
      }
    }

    @Test
    fun `When a new bedspace is created with no notes then it defaults to empty`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("invalid-params[0].propertyName").isEqualTo("$.reference")
          .jsonPath("invalid-params[0].errorType").isEqualTo("empty")
      }
    }

    @Test
    fun `When create a new bedspace with start date before premises start date returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = "",
          startDate = premises.startDate.minusDays(3),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isBadRequest
          .expectBody()
          .jsonPath("$.title").isEqualTo("Bad Request")
          .jsonPath("$.invalid-params[0].propertyName").isEqualTo("$.startDate")
          .jsonPath("$.invalid-params[0].errorType").isEqualTo("startDateBeforePremisesStartDate")
          .jsonPath("$.invalid-params[0].entityId").isEqualTo(premises.id.toString())
          .jsonPath("$.invalid-params[0].value").isEqualTo(premises.startDate.toString())
      }
    }

    @Test
    fun `When create a new bedspace with an unknown characteristic returns 400`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(7),
          startDate = LocalDate.now(),
          characteristicIds = mutableListOf(UUID.randomUUID()),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
    fun `Create new bedspace for a Premises that's not in the user's region returns 403 Forbidden`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        val premises = givenACas3Premises()
        val newBedspace = Cas3NewBedspace(
          reference = randomStringMultiCaseWithNumbers(10),
          startDate = LocalDate.now(),
          characteristicIds = emptyList(),
        )

        webTestClient.post()
          .uri("/cas3/v2/premises/${premises.id}/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
          .bodyValue(newBedspace)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }

  @Nested
  inner class GetBedspaces {
    @Test
    fun `Given a premises with bedspaces when get premises bedspaces then returns OK with correct bedspaces sorted`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )
        val expectedBedspaces = listOf(
          // online bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(6))
            withEndDate(null)
            withCreatedAt(OffsetDateTime.now().minusMonths(7))
          }.let {
            createCas3Bedspace(it, Cas3BedspaceStatus.online)
          },
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(5))
            withEndDate(LocalDate.now().plusDays(5))
            withCreatedAt(OffsetDateTime.now().minusMonths(5))
          }.let {
            createCas3Bedspace(it, Cas3BedspaceStatus.online)
          },
          // upcoming bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().plusDays(5))
            withEndDate(null)
          }.let {
            createCas3Bedspace(it, Cas3BedspaceStatus.upcoming, scheduleUnarchiveDate = it.startDate)
          },
          // archived bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(4))
            withEndDate(LocalDate.now().minusDays(1))
          }.let {
            createCas3Bedspace(it, Cas3BedspaceStatus.archived)
          },
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(9))
            withEndDate(LocalDate.now().minusWeeks(1))
          }.let {
            createCas3Bedspace(it, Cas3BedspaceStatus.archived)
          },
        )

        val expectedCas3Bedspaces = Cas3Bedspaces(
          bedspaces = expectedBedspaces,
          totalOnlineBedspaces = 2,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 2,
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/v2/premises/${premises.id}/bedspaces",
          expectedCas3Bedspaces,
        )
      }
    }

    @Test
    fun `Given a premises with bedspaces when get premises bedspaces then returns OK with correct bedspaces and archive history events in chronological order`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
        val premises = givenACas3Premises(
          user.probationRegion,
          status = Cas3PremisesStatus.online,
        )

        val expectedBedspaces = listOf(
          // online bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusWeeks(1))
            withEndDate(null)
          }.let {
            getExpectedBedspaceWithArchiveHistory(
              bedspace = it,
              premises.id,
              user.id,
              Cas3BedspaceStatus.online,
              history = listOf(
                Cas3BedspaceStatus.archived to LocalDate.now().minusMonths(1),
                Cas3BedspaceStatus.online to LocalDate.now().minusWeeks(1),
              ),
            )
          },
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusDays(5))
            withEndDate(LocalDate.now().plusDays(5))
          }.let {
            val archiveBedspaceInFiveDays = LocalDate.now().plusDays(5)
            createBedspaceArchiveDomainEvent(bedspaceId = it.id, premises.id, user.id, currentEndDate = null, endDate = archiveBedspaceInFiveDays)
            getExpectedBedspaceWithArchiveHistory(
              bedspace = it,
              premises.id,
              user.id,
              Cas3BedspaceStatus.online,
              history = listOf(
                Cas3BedspaceStatus.archived to LocalDate.now().minusMonths(2),
                Cas3BedspaceStatus.online to LocalDate.now().minusMonths(1),
              ),
            )
          },
          // upcoming bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().randomDateAfter(30))
            withEndDate(null)
          }.let {
            createCas3Bedspace(bedspace = it, Cas3BedspaceStatus.upcoming, scheduleUnarchiveDate = it.startDate)
          },
          // archived bedspaces
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(4))
            withEndDate(LocalDate.now().minusDays(1))
          }.let {
            getExpectedBedspaceWithArchiveHistory(
              bedspace = it,
              premises.id,
              user.id,
              Cas3BedspaceStatus.archived,
              history = listOf(
                Cas3BedspaceStatus.online to LocalDate.now().minusWeeks(2),
                Cas3BedspaceStatus.archived to LocalDate.now().minusDays(1),
              ),
            )
          },
          cas3BedspaceEntityFactory.produceAndPersist {
            withPremises(premises)
            withStartDate(LocalDate.now().minusMonths(9))
            withEndDate(LocalDate.now())
          }.let {
            getExpectedBedspaceWithArchiveHistory(
              bedspace = it,
              premises.id,
              user.id,
              Cas3BedspaceStatus.archived,
              listOf(
                Cas3BedspaceStatus.archived to LocalDate.now(),
              ),
            )
          },
        )

        val expectedCas3Bedspaces = Cas3Bedspaces(
          bedspaces = expectedBedspaces,
          totalOnlineBedspaces = 2,
          totalUpcomingBedspaces = 1,
          totalArchivedBedspaces = 2,
        )

        assertUrlReturnsBedspaces(
          jwt,
          "/cas3/v2/premises/${premises.id}/bedspaces",
          expectedCas3Bedspaces,
        )
      }
    }

    @Test
    fun `Get Bedspaces by ID returns Not Found with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->

        val premisesId = UUID.randomUUID().toString()

        webTestClient.get()
          .uri("/cas3/v2/premises/$premisesId/bedspaces")
          .headers(buildTemporaryAccommodationHeaders(jwt))
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
      givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
        givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { user2, _ ->
          val premises = givenACas3Premises(
            user2.probationRegion,
            status = Cas3PremisesStatus.online,
          )

          webTestClient.get()
            .uri("/cas3/v2/premises/${premises.id}/bedspaces")
            .headers(buildTemporaryAccommodationHeaders(jwt))
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
        .headers(buildTemporaryAccommodationHeaders(jwt))
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
      bedspace: Cas3BedspacesEntity,
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
        status,
        archiveHistory = history.map { Cas3BedspaceArchiveAction(it.first, it.second) },
      )
    }
  }
}
