package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
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
  decisionMadeAt: OffsetDateTime? = null,
  reallocated: Boolean = false,
  placementType: PlacementType? = PlacementType.ADDITIONAL_PLACEMENT,
  apArea: ApAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  name: String? = null,
  requiredQualification: UserQualification? = null,
  noticeType: Cas1ApplicationTimelinessCategory? = null,
): PlacementApplicationEntity {
  val userApArea = apAreaEntityFactory.produceAndPersist()

  val assessmentAllocatedToUser = userEntityFactory.produceAndPersist {
    withYieldedProbationRegion {
      probationRegionEntityFactory.produceAndPersist {
        withApArea(userApArea)
      }
    }
    withApArea(userApArea)
  }

  val assessmentCreatedByUser = userEntityFactory.produceAndPersist {
    withYieldedProbationRegion {
      probationRegionEntityFactory.produceAndPersist {
        withApArea(userApArea)
      }
    }
    withApArea(userApArea)
  }

  val (_, application) = `Given an Assessment for Approved Premises`(
    decision = assessmentDecision,
    submittedAt = OffsetDateTime.now(),
    crn = crn,
    allocatedToUser = assessmentAllocatedToUser,
    createdByUser = assessmentCreatedByUser,
    apArea = apArea,
    name = name,
    requiredQualification = requiredQualification,
    noticeType = noticeType,
  )

  return placementApplicationFactory.produceAndPersist {
    withCreatedByUser(createdByUser)
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withSchemaVersion(schema)
    withSubmittedAt(submittedAt)
    withDecision(decision)
    withDecisionMadeAt(decisionMadeAt)
    withPlacementType(placementType!!)
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withDueAt(dueAt)
  }
}

@Suppress("LongParameterList")
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
): PlacementApplicationEntity {
  val placementApplication = `Given a Placement Application`(
    assessmentDecision = assessmentDecision,
    createdByUser = createdByUser,
    schema = schema,
    crn = crn,
    allocatedToUser = allocatedToUser,
    submittedAt = submittedAt,
    decision = decision,
    reallocated = reallocated,
    placementType = placementType,
    dueAt = dueAt,
  )
  block(placementApplication)
  return placementApplication
}
