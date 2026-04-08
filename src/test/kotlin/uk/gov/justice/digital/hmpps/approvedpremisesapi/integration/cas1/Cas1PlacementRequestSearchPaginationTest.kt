package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenSomeOffenders
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementRequestSearchPaginationTest : IntegrationTestBase() {

  @Test
  fun `search with pagination is deterministic when multiple records have the same sort field value`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      givenSomeOffenders { offenderSequence ->
        val createdAt = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        val offenders = offenderSequence.take(15).toList()

        apDeliusContextAddListCaseSummaryToBulkResponse(offenders.map { it.first.asCaseSummary() })

        offenders.forEach { (offender, _) ->
          createPlacementRequest(offender.asCaseSummary(), user, createdAt = createdAt, applicationDate = createdAt)
        }

        // Fetch page 1 (default size 10)
        val page1 = webTestClient.get()
          .uri("/cas1/placement-requests?page=1&sortBy=createdAt&sortDirection=asc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

        // Fetch page 2
        val page2 = webTestClient.get()
          .uri("/cas1/placement-requests?page=2&sortBy=createdAt&sortDirection=asc")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

        val allIds = (page1 + page2).map { it.id }
        assertThat(allIds).hasSize(15)
        assertThat(allIds.distinct()).hasSize(15)

        // Repeat multiple times to increase chance of catching non-determinism (simulates refresh)
        repeat(15) { i ->
          val page1Repeat = webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=createdAt&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          val page2Repeat = webTestClient.get()
            .uri("/cas1/placement-requests?page=2&sortBy=createdAt&sortDirection=asc")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(page1.map { it.id })
            .withFailMessage("Sort order was non-deterministic on refresh $i for page 1")
            .containsExactlyElementsOf(page1Repeat.map { it.id })
          assertThat(page2.map { it.id })
            .withFailMessage("Sort order was non-deterministic on refresh $i for page 2")
            .containsExactlyElementsOf(page2Repeat.map { it.id })
        }
      }
    }
  }

  @Test
  fun `search with pagination is deterministic when filtered by tier`() {
    givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
      givenSomeOffenders { offenderSequence ->
        val createdAt = OffsetDateTime.parse("2024-01-01T10:00:00Z")
        val allOffenders = offenderSequence.take(20).toList()
        val b1Offenders = allOffenders.take(15)
        val c1Offenders = allOffenders.drop(15).take(5)

        apDeliusContextAddListCaseSummaryToBulkResponse((b1Offenders + c1Offenders).map { it.first.asCaseSummary() })

        // Create 15 placement requests with the same tier and createdAt
        b1Offenders.forEach { (offender, _) ->
          createPlacementRequest(offender.asCaseSummary(), user, createdAt = createdAt, tier = RiskTierLevel.b1)
        }

        // Create 5 more with a different tier
        c1Offenders.forEach { (offender, _) ->
          createPlacementRequest(offender.asCaseSummary(), user, createdAt = createdAt, tier = RiskTierLevel.c1)
        }

        // Fetch page 1 (default size 10) for tier B1
        val page1 = webTestClient.get()
          .uri("/cas1/placement-requests?page=1&sortBy=createdAt&sortDirection=asc&tier=B1")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

        // Fetch page 2 for tier B1
        val page2 = webTestClient.get()
          .uri("/cas1/placement-requests?page=2&sortBy=createdAt&sortDirection=asc&tier=B1")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

        val allIds = (page1 + page2).map { it.id }
        assertThat(allIds).hasSize(15)
        assertThat(allIds.distinct()).hasSize(15)

        repeat(15) { i ->
          val page1Repeat = webTestClient.get()
            .uri("/cas1/placement-requests?page=1&sortBy=createdAt&sortDirection=asc&tier=B1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          val page2Repeat = webTestClient.get()
            .uri("/cas1/placement-requests?page=2&sortBy=createdAt&sortDirection=asc&tier=B1")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas1PlacementRequestSummary>()

          assertThat(page1.map { it.id })
            .withFailMessage("Sort order was non-deterministic on refresh $i for page 1")
            .containsExactlyElementsOf(page1Repeat.map { it.id })
          assertThat(page2.map { it.id })
            .withFailMessage("Sort order was non-deterministic on refresh $i for page 2")
            .containsExactlyElementsOf(page2Repeat.map { it.id })
        }
      }
    }
  }

  private fun createPlacementRequest(
    caseSummary: CaseSummary,
    user: UserEntity,
    createdAt: OffsetDateTime = OffsetDateTime.now(),
    applicationDate: OffsetDateTime = OffsetDateTime.now(),
    tier: RiskTierLevel = RiskTierLevel.b1,
  ): PlacementRequestEntity {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(caseSummary.crn)
      withCreatedByUser(user)
      withSubmittedAt(applicationDate)
      withReleaseType("licence")
      withName("${caseSummary.name.forename} ${caseSummary.name.surname}")
      withRiskRatings(
        uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory()
          .withTier(
            RiskWithStatus(
              RiskTier(
                level = tier.value,
                lastUpdated = LocalDate.parse("2023-06-26"),
              ),
            ),
          ).produce(),
      )
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withSubmittedAt(OffsetDateTime.now())
      withAllocatedToUser(user)
      withDecision(AssessmentDecision.ACCEPTED)
    }

    val placementRequirements = placementRequirementsFactory.produceAndPersist {
      withApplication(application)
      withAssessment(assessment)
      withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
      withDesirableCriteria(
        characteristicEntityFactory.produceAndPersistMultiple(1),
      )
      withEssentialCriteria(
        characteristicEntityFactory.produceAndPersistMultiple(1),
      )
    }

    return placementRequestFactory.produceAndPersist {
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
      withDuration(7)
      withExpectedArrival(LocalDate.now())
      withCreatedAt(createdAt)
      withIsWithdrawn(false)
      withIsParole(false)
    }
  }
}
