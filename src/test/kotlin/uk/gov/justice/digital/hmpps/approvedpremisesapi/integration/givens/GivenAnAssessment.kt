package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.`Given an Assessment for Approved Premises`(
  allocatedToUser: UserEntity?,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  decision: AssessmentDecision? = null,
  submittedAt: OffsetDateTime? = null,
  createdAt: OffsetDateTime? = null,
  isWithdrawn: Boolean = false,
): Pair<AssessmentEntity, ApprovedPremisesApplicationEntity> {
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
    withIsWithdrawn(isWithdrawn)
  }

  assessment.schemaUpToDate = true

  return Pair(assessment, application)
}

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.`Given an Assessment for Approved Premises`(
  allocatedToUser: UserEntity?,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  decision: AssessmentDecision? = null,
  submittedAt: OffsetDateTime? = null,
  block: (assessment: AssessmentEntity, application: ApprovedPremisesApplicationEntity) -> Unit,
) {
  val (assessment, application) = `Given an Assessment for Approved Premises`(
    allocatedToUser,
    createdByUser,
    crn,
    reallocated,
    data,
    decision,
    submittedAt,
  )

  block(assessment, application)
}

fun IntegrationTestBase.`Given an Assessment for Temporary Accommodation`(
  allocatedToUser: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  data: String? = "{ \"some\": \"data\"}",
  createdAt: OffsetDateTime? = null,
  block: ((assessment: AssessmentEntity, application: TemporaryAccommodationApplicationEntity) -> Unit)? = null,
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
    withAllocatedToUser(allocatedToUser)
    withApplication(application)
    withAssessmentSchema(assessmentSchema)
    withData(data)
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
