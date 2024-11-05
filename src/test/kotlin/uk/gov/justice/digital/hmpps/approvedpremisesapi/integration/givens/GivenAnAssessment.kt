package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.givenAnAssessmentForApprovedPremises(
  allocatedToUser: UserEntity?,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  decision: AssessmentDecision? = null,
  submittedAt: OffsetDateTime? = null,
  createdAt: OffsetDateTime? = null,
  isWithdrawn: Boolean = false,
  apArea: ApAreaEntity? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  name: String? = null,
  requiredQualification: UserQualification? = null,
  noticeType: Cas1ApplicationTimelinessCategory? = null,
  createdFromAppeal: Boolean = false,
): Pair<ApprovedPremisesAssessmentEntity, ApprovedPremisesApplicationEntity> {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
    withAddedAt(OffsetDateTime.now())
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(OffsetDateTime.now())
    withReleaseType("licence")
    withIsWithdrawn(isWithdrawn)
    withApArea(apArea)
    withCruManagementArea(cruManagementArea)
    if (name !== null) {
      withName(name)
    }
    when (requiredQualification) {
      UserQualification.PIPE -> withApType(ApprovedPremisesType.PIPE)
      UserQualification.ESAP -> withApType(ApprovedPremisesType.ESAP)
      UserQualification.RECOVERY_FOCUSED -> withApType(ApprovedPremisesType.RFAP)
      UserQualification.MENTAL_HEALTH_SPECIALIST -> withApType(ApprovedPremisesType.MHAP_ST_JOSEPHS)
      else -> { }
    }
    withNoticeType(noticeType)
    withStatus(
      when (decision) {
        AssessmentDecision.ACCEPTED -> ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT
        AssessmentDecision.REJECTED -> ApprovedPremisesApplicationStatus.REJECTED
        null -> ApprovedPremisesApplicationStatus.STARTED
      },
    )
  }

  val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    if (allocatedToUser != null) {
      withAllocatedToUser(allocatedToUser)
    }
    withApplication(application)
    withAssessmentSchema(assessmentSchema)
    withData(data)
    withDecision(decision)
    withSubmittedAt(submittedAt)
    if (createdAt != null) {
      withCreatedAt(createdAt)
    }
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withCreatedFromAppeal(createdFromAppeal)
    withIsWithdrawn(isWithdrawn)
    withDueAt(dueAt)
  }

  assessment.schemaUpToDate = true

  return Pair(assessment, application)
}

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.givenAnAssessmentForApprovedPremises(
  allocatedToUser: UserEntity?,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  decision: AssessmentDecision? = null,
  submittedAt: OffsetDateTime? = null,
  dueAt: OffsetDateTime? = null,
  createdFromAppeal: Boolean = false,
  block: (assessment: ApprovedPremisesAssessmentEntity, application: ApprovedPremisesApplicationEntity) -> Unit,
) {
  val (assessment, application) = givenAnAssessmentForApprovedPremises(
    allocatedToUser = allocatedToUser,
    createdByUser = createdByUser,
    crn = crn,
    reallocated = reallocated,
    data = data,
    decision = decision,
    submittedAt = submittedAt,
    dueAt = dueAt,
    createdFromAppeal = createdFromAppeal,
  )

  block(assessment, application)
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenAnAssessmentForTemporaryAccommodation(
  allocatedToUser: UserEntity?,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  createdAt: OffsetDateTime? = null,
  block: ((assessment: TemporaryAccommodationAssessmentEntity, application: TemporaryAccommodationApplicationEntity) -> Unit)? = null,
) {
  val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
    withAddedAt(OffsetDateTime.now())
  }

  val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
    withAddedAt(OffsetDateTime.now())
  }

  val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withProbationRegion(createdByUser.probationRegion)
  }

  val assessment = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    if (allocatedToUser != null) {
      withAllocatedToUser(allocatedToUser)
    }
    withApplication(application)
    withAssessmentSchema(assessmentSchema)
    withData(data)
    withReleaseDate(null)
    withAccommodationRequiredFromDate(null)
    if (createdAt != null) {
      withCreatedAt(createdAt)
    }
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
  }

  assessment.schemaUpToDate = true

  block?.invoke(assessment, application)
}
