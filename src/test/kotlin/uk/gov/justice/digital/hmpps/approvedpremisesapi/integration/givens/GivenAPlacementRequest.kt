package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  block: (placementRequest: PlacementRequestEntity, application: ApplicationEntity) -> Unit
) {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
  }

  val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val assessment = assessmentEntityFactory.produceAndPersist {
    withAssessmentSchema(assessmentSchema)
    withApplication(application)
    withSubmittedAt(OffsetDateTime.now())
    withAllocatedToUser(placementRequestAllocatedTo)
    withDecision(AssessmentDecision.ACCEPTED)
  }

  val placementRequest = placementRequestFactory.produceAndPersist {
    withAllocatedToUser(placementRequestAllocatedTo)
    withApplication(application)
    withAssessment(assessment)
    withPostcodeDistrict(
      postCodeDistrictRepository.findAll()[0]
    )
    withDesirableCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(5)
    )
    withEssentialCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(3)
    )
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
  }

  block(placementRequest, application)
}
