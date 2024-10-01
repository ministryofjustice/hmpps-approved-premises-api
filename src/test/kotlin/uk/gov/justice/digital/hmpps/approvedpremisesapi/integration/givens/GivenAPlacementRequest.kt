package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity?,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String? = null,
  name: String? = null,
  reallocated: Boolean = false,
  isWithdrawn: Boolean = false,
  withdrawalReason: PlacementRequestWithdrawalReason? = null,
  isParole: Boolean = false,
  expectedArrival: LocalDate? = null,
  tier: String? = null,
  mappa: String? = null,
  applicationSubmittedAt: OffsetDateTime = OffsetDateTime.now(),
  booking: BookingEntity? = null,
  apArea: ApAreaEntity? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  duration: Int? = null,
  assessmentSubmittedAt: OffsetDateTime = OffsetDateTime.now(),
  placementApplication: PlacementApplicationEntity? = null,
  requiredQualification: UserQualification? = null,
  noticeType: Cas1ApplicationTimelinessCategory? = null,
  application: ApprovedPremisesApplicationEntity? = null,
): Pair<PlacementRequestEntity, ApprovedPremisesApplicationEntity> {
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
          lastUpdated = LocalDate.now(),
        ),
      ),
    )
  }

  val app = application ?: approvedPremisesApplicationEntityFactory.produceAndPersist {
    crn?.let { withCrn(it) }
    name?.let { withName(it) }
    withCreatedByUser(createdByUser)
    withApplicationSchema(applicationSchema)
    withSubmittedAt(applicationSubmittedAt)
    withReleaseType("licence")
    withRiskRatings(
      risksFactory.produce(),
    )
    withApArea(apArea)
    withCruManagementArea(cruManagementArea)
    applyQualification(requiredQualification)
    withNoticeType(noticeType)
  }

  val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
    withAssessmentSchema(assessmentSchema)
    withApplication(app)
    withSubmittedAt(assessmentSubmittedAt)
    withAllocatedToUser(assessmentAllocatedTo)
    withDecision(AssessmentDecision.ACCEPTED)
  }

  val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

  val placementRequirements = placementRequirementsFactory.produceAndPersist {
    withApplication(app)
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
    if (placementRequestAllocatedTo != null) {
      withAllocatedToUser(placementRequestAllocatedTo)
    }
    withApplication(app)
    withAssessment(assessment)
    if (reallocated) {
      withReallocatedAt(OffsetDateTime.now())
    }
    withIsWithdrawn(isWithdrawn)
    withWithdrawalReason(withdrawalReason)
    withIsParole(isParole)
    withPlacementRequirements(placementRequirements)
    if (booking != null) {
      withBooking(booking)
    }
    if (expectedArrival != null) {
      withExpectedArrival(expectedArrival)
    }
    if (duration != null) {
      withDuration(duration)
    }
    withDueAt(dueAt)
    if (placementApplication != null) {
      withPlacementApplication(placementApplication)
    }
  }

  return Pair(placementRequest, app)
}

private fun ApprovedPremisesApplicationEntityFactory.applyQualification(requiredQualification: UserQualification?) {
  when (requiredQualification) {
    UserQualification.PIPE -> withApType(ApprovedPremisesType.PIPE)
    UserQualification.ESAP -> withApType(ApprovedPremisesType.ESAP)
    UserQualification.RECOVERY_FOCUSED -> withApType(ApprovedPremisesType.RFAP)
    UserQualification.MENTAL_HEALTH_SPECIALIST -> withApType(ApprovedPremisesType.MHAP_ST_JOSEPHS)
    else -> { }
  }
}

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.`Given a Placement Request`(
  placementRequestAllocatedTo: UserEntity?,
  assessmentAllocatedTo: UserEntity,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
  reallocated: Boolean = false,
  expectedArrival: LocalDate? = null,
  tier: String? = null,
  isWithdrawn: Boolean = false,
  withdrawalReason: PlacementRequestWithdrawalReason? = null,
  apArea: ApAreaEntity? = null,
  cruManagementArea: Cas1CruManagementAreaEntity? = null,
  dueAt: OffsetDateTime? = OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres(),
  duration: Int? = null,
  applicationSubmittedAt: OffsetDateTime = OffsetDateTime.now(),
  assessmentSubmittedAt: OffsetDateTime = OffsetDateTime.now(),
  placementApplication: PlacementApplicationEntity? = null,
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
    isWithdrawn = isWithdrawn,
    withdrawalReason = withdrawalReason,
    apArea = apArea,
    cruManagementArea = cruManagementArea,
    dueAt = dueAt,
    duration = duration,
    applicationSubmittedAt = applicationSubmittedAt,
    assessmentSubmittedAt = assessmentSubmittedAt,
    placementApplication = placementApplication,
  )

  block(result.first, result.second)
}
