package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime

fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String? = null,
  name: String? = null,
  reallocated: Boolean = false,
  isWithdrawn: Boolean = false,
  isParole: Boolean = false,
  expectedArrival: LocalDate? = null,
  tier: String? = null,
  mappa: String? = null,
  applicationSubmittedAt: OffsetDateTime = OffsetDateTime.now(),
): Pair<PlacementRequestEntity, ApplicationEntity> {
  val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  var risksFactory = PersonRisksFactory()

  if (tier != null) {
    risksFactory = risksFactory.withTier(
      RiskWithStatus(
        RiskTier(tier, LocalDate.now()),
      ),
    )
  }

  if (mappa != null) {
    risksFactory = risksFactory.withMappa(
      RiskWithStatus(
        status = RiskStatus.Retrieved,
        value = Mappa(
          level = "CAT M2/LEVEL M2",
          lastUpdated = java.time.LocalDate.now(),
        ),
      ),
    )
  }

  val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
    crn?.let { withCrn(it) }
    name?.let { withName(it) }
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(applicationSubmittedAt)
    withReleaseType("licence")
    withRiskRatings(
      risksFactory.produce(),
    )
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

    if (expectedArrival != null) {
      withExpectedArrival(expectedArrival)
    }
  }

  return Pair(placementRequest, application)
}

fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  expectedArrival: LocalDate? = null,
  tier: String? = null,
  block: (placementRequest: PlacementRequestEntity, application: ApplicationEntity) -> Unit,
) {
  val result = `Given a Placement Request`(
    placementRequestAllocatedTo,
    assessmentAllocatedTo,
    createdByUser,
    crn,
    reallocated = reallocated,
    expectedArrival = expectedArrival,
    tier = tier,
  )

  block(result.first, result.second)
}
