package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Application`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a Placement Request`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.time.OffsetDateTime

class MigrateCas1FixPlacementApplicationLinksTest : MigrationJobTestBase() {

  // Due to lack of feedback from migration jobs it's hard to prove the query
  // we use to retrieve candidate applications works. For this reason we test
  // it directly
  @Nested
  inner class QueryTest {

    @Test
    fun `Repo query only returns relevant applications`() {
      `Given a User` { user, _ ->

        `Given a Placement Application`(
          crn = "placementAppWithoutDecision",
          createdByUser = user,
          decision = null,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
        )

        val placementAppAlreadyLinkedToPlacementRequest = `Given a Placement Application`(
          crn = "placementAppAlreadyLinkedToPlacementRequest",
          createdByUser = user,
          decision = PlacementApplicationDecision.ACCEPTED,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
        )
        `Given a Placement Request`(
          crn = placementAppAlreadyLinkedToPlacementRequest.application.crn,
          placementRequestAllocatedTo = user,
          assessmentAllocatedTo = user,
          createdByUser = user,
          placementApplication = placementAppAlreadyLinkedToPlacementRequest,
        )

        val placementAppNotLinkedToPlacementRequest = `Given a Placement Application`(
          crn = "placementAppNotLinkedToPlacementRequest",
          createdByUser = user,
          decision = PlacementApplicationDecision.ACCEPTED,
          schema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }
        )

        val result =
          placementApplicationRepository.findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(placementAppNotLinkedToPlacementRequest.application.id.toString())
      }
    }

  }

}