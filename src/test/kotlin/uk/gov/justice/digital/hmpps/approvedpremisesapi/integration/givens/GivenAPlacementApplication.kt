package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAPlacementApplication(
  assessmentDecision: AssessmentDecision = AssessmentDecision.ACCEPTED,
  createdByUser: UserEntity,
  schema: ApprovedPremisesPlacementApplicationJsonSchemaEntity? = null,
  crn: String = randomStringMultiCaseWithNumbers(8),
  allocatedToUser: UserEntity? = null,
  submittedAt: OffsetDateTime? = null,
  decision: PlacementApplicationDecision? = null,
  decisionMadeAt: OffsetDateTime? = null,
  reallocated: Boolean = false,
  placementType: PlacementType? = PlacementType.ADDITIONAL_PLACEMENT,
  apArea: ApAreaEntity? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  name: String? = null,
  requiredQualification: UserQualification? = null,
  noticeType: Cas1ApplicationTimelinessCategory? = null,
  isWithdrawn: Boolean = false,
  placementDates: List<PlacementDateEntity> = mutableListOf(),
  application: ApprovedPremisesApplicationEntity? = null,
): PlacementApplicationEntity {
  val userApArea = givenAnApArea()

  val (assessmentAllocatedToUser) = givenAUser(
    probationRegion = givenAProbationRegion(apArea = userApArea),
  )

  val assessmentCreatedByUser = userEntityFactory.produceAndPersist {
    withYieldedProbationRegion {
      probationRegionEntityFactory.produceAndPersist {
        withApArea(userApArea)
      }
    }
    withApArea(userApArea)
  }

  val application = application ?: givenAnAssessmentForApprovedPremises(
    decision = assessmentDecision,
    submittedAt = OffsetDateTime.now(),
    crn = crn,
    allocatedToUser = assessmentAllocatedToUser,
    createdByUser = assessmentCreatedByUser,
    apArea = apArea,
    cruManagementArea = cruManagementArea,
    name = name,
    requiredQualification = requiredQualification,
    noticeType = noticeType,
  ).second

  return placementApplicationFactory.produceAndPersist {
    withCreatedByUser(createdByUser)
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withSchemaVersion(
      schema ?: approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      },
    )
    withSubmittedAt(submittedAt)
    withDecision(decision)
    withDecisionMadeAt(decisionMadeAt)
    withPlacementType(placementType!!)
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withDueAt(dueAt)
    withIsWithdrawn(isWithdrawn)
    withPlacementDates(placementDates.toMutableList())
  }
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAPlacementApplication(
  assessmentDecision: AssessmentDecision = AssessmentDecision.ACCEPTED,
  createdByUser: UserEntity = givenAUser().first,
  schema: ApprovedPremisesPlacementApplicationJsonSchemaEntity? = null,
  crn: String = randomStringMultiCaseWithNumbers(8),
  allocatedToUser: UserEntity? = null,
  submittedAt: OffsetDateTime? = null,
  decision: PlacementApplicationDecision? = null,
  reallocated: Boolean = false,
  placementType: PlacementType? = PlacementType.ADDITIONAL_PLACEMENT,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  placementDates: MutableList<PlacementDateEntity> = mutableListOf(),
  application: ApprovedPremisesApplicationEntity? = null,
  block: (placementApplicationEntity: PlacementApplicationEntity) -> Unit = { },
): PlacementApplicationEntity {
  val placementApplication = givenAPlacementApplication(
    assessmentDecision = assessmentDecision,
    createdByUser = createdByUser,
    schema = schema ?: approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    },
    crn = crn,
    allocatedToUser = allocatedToUser,
    submittedAt = submittedAt,
    decision = decision,
    reallocated = reallocated,
    placementType = placementType,
    dueAt = dueAt,
    placementDates = placementDates,
    application = application,
    isWithdrawn = false,
  )
  block(placementApplication)
  return placementApplication
}
