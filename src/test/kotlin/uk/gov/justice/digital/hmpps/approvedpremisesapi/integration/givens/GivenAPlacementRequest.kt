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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDate
import java.time.OffsetDateTime

@Suppress(
  "LongParameterList",
)
fun IntegrationTestBase.givenAPlacementRequest(
  assessmentAllocatedTo: UserEntity? = null,
  createdByUser: UserEntity = givenAUser().first,
  crn: String? = null,
  name: String? = null,
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
  essentialCriteria: List<CharacteristicEntity>? = null,
  caseManager: Cas1ApplicationUserDetailsEntity? = null,
  apType: ApprovedPremisesType? = null,
  applicationCreatedAt: OffsetDateTime = OffsetDateTime.now().randomDateTimeBefore(30),
  placementRequestCreatedAt: OffsetDateTime = OffsetDateTime.now(),
): Pair<PlacementRequestEntity, ApprovedPremisesApplicationEntity> {
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
    withCreatedAt(applicationCreatedAt)
    withCreatedByUser(createdByUser)
    withSubmittedAt(applicationSubmittedAt)
    withReleaseType("licence")
    withRiskRatings(
      risksFactory.produce(),
    )
    withApArea(apArea)
    withCruManagementArea(cruManagementArea)
    applyQualification(requiredQualification)
    withNoticeType(noticeType)
    withCaseManagerUserDetails(caseManager)
    withCaseManagerIsNotApplicant(caseManager != null)
    apType?.let { withApType(it) }
  }

  val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
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
      essentialCriteria ?: characteristicEntityFactory.produceAndPersistMultiple(3),
    )
  }

  val placementRequest = placementRequestFactory.produceAndPersist {
    withApplication(app)
    withAssessment(assessment)
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
    withCreatedAt(placementRequestCreatedAt)
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
fun IntegrationTestBase.givenAPlacementRequest(
  assessmentAllocatedTo: UserEntity? = null,
  createdByUser: UserEntity,
  crn: String = randomStringMultiCaseWithNumbers(8),
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
  caseManager: Cas1ApplicationUserDetailsEntity? = null,
  block: (placementRequest: PlacementRequestEntity, application: ApplicationEntity) -> Unit,
): Pair<PlacementRequestEntity, ApprovedPremisesApplicationEntity> {
  val result = givenAPlacementRequest(
    assessmentAllocatedTo,
    createdByUser,
    crn,
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
    caseManager = caseManager,
  )

  block(result.first, result.second)

  return result
}
