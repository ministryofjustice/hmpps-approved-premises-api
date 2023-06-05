package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a Placement Application`(
  assessmentDecision: AssessmentDecision = AssessmentDecision.ACCEPTED,
  createdByUser: UserEntity,
  schema: ApprovedPremisesPlacementApplicationJsonSchemaEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  allocatedToUser: UserEntity? = null,
  submittedAt: OffsetDateTime? = null,
  decision: PlacementApplicationDecision? = null,
  reallocated: Boolean = false,
  block: (placementApplicationEntity: PlacementApplicationEntity) -> Unit,
) {
  `Given an Assessment for Approved Premises`(
    decision = assessmentDecision,
    submittedAt = OffsetDateTime.now(),
    crn = crn,
    allocatedToUser = userEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    },
    createdByUser = userEntityFactory.produceAndPersist {
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    },
  ) { _, application ->
    val placementApplicationEntity = placementApplicationFactory.produceAndPersist {
      withCreatedByUser(createdByUser)
      withAllocatedToUser(allocatedToUser)
      withApplication(application)
      withSchemaVersion(schema)
      withSubmittedAt(submittedAt)
      withDecision(decision)
      withPlacementType(PlacementType.ADDITIONAL_PLACEMENT)
      if (reallocated) {
        withReallocatedAt(OffsetDateTime.now())
      }
    }

    block(placementApplicationEntity)
  }
}
