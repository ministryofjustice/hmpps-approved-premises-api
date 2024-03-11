package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.`Given a Placement Application`(
  assessmentDecision: AssessmentDecision = AssessmentDecision.ACCEPTED,
  createdByUser: UserEntity,
  schema: ApprovedPremisesPlacementApplicationJsonSchemaEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  allocatedToUser: UserEntity? = null,
  submittedAt: OffsetDateTime? = null,
  decision: PlacementApplicationDecision? = null,
  reallocated: Boolean = false,
  placementType: PlacementType? = PlacementType.ADDITIONAL_PLACEMENT,
  apArea: ApAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  name: String? = null,
): PlacementApplicationEntity {
  val (_, application) = `Given an Assessment for Approved Premises`(
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
    apArea = apArea,
    name = name,
  )

  return placementApplicationFactory.produceAndPersist {
    withCreatedByUser(createdByUser)
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withSchemaVersion(schema)
    withSubmittedAt(submittedAt)
    withDecision(decision)
    withPlacementType(placementType!!)
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withDueAt(dueAt)
  }
}

fun IntegrationTestBase.`Given a Placement Application`(
  assessmentDecision: AssessmentDecision = AssessmentDecision.ACCEPTED,
  createdByUser: UserEntity,
  schema: ApprovedPremisesPlacementApplicationJsonSchemaEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  allocatedToUser: UserEntity? = null,
  submittedAt: OffsetDateTime? = null,
  decision: PlacementApplicationDecision? = null,
  reallocated: Boolean = false,
  placementType: PlacementType? = PlacementType.ADDITIONAL_PLACEMENT,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  block: (placementApplicationEntity: PlacementApplicationEntity) -> Unit,
) {
  block(
    `Given a Placement Application`(
      assessmentDecision,
      createdByUser,
      schema,
      crn,
      allocatedToUser,
      submittedAt,
      decision,
      reallocated,
      placementType,
      dueAt = dueAt,
    ),
  )
}
