package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  isWithdrawn: Boolean = false,
  isParole: Boolean = false,
  tier: String? = null,
): Pair<PlacementRequestEntity, ApplicationEntity> {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(OffsetDateTime.now())
    withReleaseType("licence")
    if (tier != null) {
      withRiskRatings(
        PersonRisksFactory()
          .withTier(
            RiskWithStatus(
              RiskTier(tier, LocalDate.now())
            )
          )
          .produce()
      )
    }
  }

  val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    withAssessmentSchema(assessmentSchema)
    withApplication(application)
    withSubmittedAt(OffsetDateTime.now())
    withAllocatedToUser(placementRequestAllocatedTo)
    withDecision(AssessmentDecision.ACCEPTED)
  }

  val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

  val placementRequirements = placementRequirementsFactory.produceAndPersist {
    withApplication(application)
    withAssessment(assessment)
    withPostcodeDistrict(postcodeDistrict)
    withDesirableCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(5),
    )
    withEssentialCriteria(
      characteristicEntityFactory.produceAndPersistMultiple(3),
    )
  }

  val placementRequest = placementRequestFactory.produceAndPersist {
    withAllocatedToUser(placementRequestAllocatedTo)
    withApplication(application)
    withAssessment(assessment)
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withIsWithdrawn(isWithdrawn)
    withIsParole(isParole)
    withPlacementRequirements(placementRequirements)
  }

  return Pair(placementRequest, application)
}

fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  tier: String? = null,
  block: (placementRequest: PlacementRequestEntity, application: ApplicationEntity) -> Unit,
) {
  val result = `Given a Placement Request`(
    placementRequestAllocatedTo,
    assessmentAllocatedTo,
    createdByUser,
    crn,
    reallocated,
    tier = tier
  )

  block(result.first, result.second)
}
