package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import java.time.LocalDate
import java.time.OffsetDateTime

class MigrateCas1FixPlacementApplicationLinksTest : MigrationJobTestBase() {

  @Nested
  inner class QueryTest {

    @Test
    fun `Repo query only returns relevant applications`() {
      givenAUser { user, _ ->

        givenAPlacementApplication(
          crn = "placementAppWithoutDecision",
          createdByUser = user,
          decision = null,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        )

        val placementAppAlreadyLinkedToPlacementRequest = givenAPlacementApplication(
          crn = "placementAppAlreadyLinkedToPlacementRequest",
          createdByUser = user,
          decision = PlacementApplicationDecision.ACCEPTED,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        )
        givenAPlacementRequest(
          crn = placementAppAlreadyLinkedToPlacementRequest.application.crn,
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          placementApplication = placementAppAlreadyLinkedToPlacementRequest,
        )

        val placementAppNotLinkedToPlacementRequest = givenAPlacementApplication(
          crn = "placementAppNotLinkedToPlacementRequest",
          createdByUser = user,
          decision = PlacementApplicationDecision.ACCEPTED,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        )

        val result =
          placementApplicationRepository.findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(placementAppNotLinkedToPlacementRequest.application.id.toString())
      }
    }
  }

  @Nested
  inner class MigrationTest {

    @Test
    fun `Backfill adds placement request to placement application link`() {
      givenAUser { user, _ ->
        val (_, application) = givenAPlacementRequest(
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          expectedArrival = LocalDate.of(2024, 5, 6),
          duration = 7,
        )

        val placementApplication = placementApplicationFactory.produceAndPersist {
          withApplication(application)
          withCreatedByUser(user)
          withSchemaVersion(
            approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
              withPermissiveSchema()
            },
          )
          withSubmittedAt(OffsetDateTime.now())
          withDecision(PlacementApplicationDecision.ACCEPTED)
        }
        placementDateFactory.produceAndPersist {
          withPlacementApplication(placementApplication)
          withExpectedArrival(LocalDate.of(2024, 5, 6))
          withDuration(7)
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas1FixPlacementAppLinks)

        val updatedPlacementRequests = placementRequestRepository.findByApplicationId(application.id)
        assertThat(updatedPlacementRequests).hasSize(1)
        assertThat(updatedPlacementRequests[0].placementApplication!!.id).isEqualTo(placementApplication.id)
      }
    }
  }
}
