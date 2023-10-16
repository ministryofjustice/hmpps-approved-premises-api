package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService

enum class Tier {
  ALL,
  A0,
  A1,
  A2,
  A3,
  B0,
  B1,
  B2,
  B3,
  C1,
  C2,
  C3,
  D2,
  NONE,
}

class ReferralsMetricsReportGenerator(
  private val assessments: List<ApprovedPremisesAssessmentEntity>,
  private val workingDayCountService: WorkingDayCountService,
) : ReportGenerator<Tier, ReferralsMetricsReportRow, ReferralsMetricsProperties>(ReferralsMetricsReportRow::class) {
  override fun filter(properties: ReferralsMetricsProperties): (Tier) -> Boolean = {
    true
  }

  override val convert: Tier.(properties: ReferralsMetricsProperties) -> List<ReferralsMetricsReportRow> = {
    val referralsForTier = if (this == Tier.ALL) {
      assessments
    } else if (this == Tier.NONE) {
      assessments.filter { (it.application as ApprovedPremisesApplicationEntity).riskRatings?.tier?.value?.level == null }
    } else {
      assessments.filter { (it.application as ApprovedPremisesApplicationEntity).riskRatings?.tier?.value?.level == this.toString() }
    }

    listOf(
      ReferralsMetricsReportRow(
        tier = this.toString(),
        numberOfUniqueReferrals = referralsForTier.size,
        referralsAccepted = referralsForTier.filter { it.decision == AssessmentDecision.ACCEPTED }.size,
        assessmentCompletionTimeliness = getCompletionTimeliness(referralsForTier),
        referralsWithInformationRequests = referralsForTier.filter { it.clarificationNotes.size > 0 }.size,
        referralsRejectedAccommodationNeedOnly = referralsWithRejectionReason(referralsForTier, "Reject, not suitable for an AP: Accommodation need only"),
        referralsRejectedNeedsCannotBeMet = referralsWithRejectionReason(referralsForTier, "Reject, not suitable for an AP: Health / social care / disability needs cannot be met"),
        referralsRejectedSupervisionPeriodTooShort = referralsWithRejectionReason(referralsForTier, "Reject, not suitable for an AP: Remaining supervision period too short"),
        referralsRejectedRiskTooLow = referralsWithRejectionReason(referralsForTier, "Reject, not suitable for an AP: Risk too low"),
        referralsRejectedOtherReasons = referralsWithRejectionReason(referralsForTier, "Reject, not suitable for an AP: Not suitable for other reasons"),
        referralsRejectedRequestedInformationNotProvided = referralsWithRejectionReason(referralsForTier, "Reject, insufficient information: Requested information not provided by probation practitioner"),
        referralsRejectedInsufficientContingencyPlan = referralsWithRejectionReason(referralsForTier, "Reject, insufficient information: Insufficient contingency plan"),
        referralsRejectedInsufficientMoveOnPlan = referralsWithRejectionReason(referralsForTier, "Reject, insufficient information: Insufficient move on plan"),
        referralsRejectedRiskTooHighToCommunity = referralsWithRejectionReason(referralsForTier, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to community"),
        referralsRejectedRiskTooHighToOtherPeopleInAP = referralsWithRejectionReason(referralsForTier, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to other people in AP"),
        referralsRejectedRiskToHighToStaff = referralsWithRejectionReason(referralsForTier, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to staff"),
        referralsWithdrawn = referralsWithRejectionReason(referralsForTier, "Application withdrawn: Application withdrawn by the probation practitioner"),
        referralsByReleaseTypeLicence = referralsWithReleaseType(referralsForTier, "licence"),
        referralsByReleaseTypeHdc = referralsWithReleaseType(referralsForTier, "hdc"),
        referralsByReleaseTypePSS = referralsWithReleaseType(referralsForTier, "pss"),
        referralsByReleaseTypeParole = referralsWithReleaseType(referralsForTier, "rotl"),
      ),
    )
  }

  private fun referralsWithRejectionReason(referrals: List<ApprovedPremisesAssessmentEntity>, rejectionReason: String) = referrals.filter {
    it.rejectionRationale == rejectionReason
  }.size

  private fun referralsWithReleaseType(referrals: List<ApprovedPremisesAssessmentEntity>, releaseType: String) = referrals.filter {
    (it.application as ApprovedPremisesApplicationEntity).releaseType == releaseType
  }.size

  private fun getCompletionTimeliness(referrals: List<ApprovedPremisesAssessmentEntity>): String {
    val totalReferrals = referrals.size

    if (totalReferrals == 0) {
      return "N/A"
    }

    val totalCompletedInTime = referrals.filter {
      val startDate = it.application.submittedAt?.toLocalDate()
      val endDate = it.submittedAt?.toLocalDate()

      (startDate !== null && endDate !== null && workingDayCountService.getWorkingDaysCount(startDate, endDate) <= 10)
    }.size

    val percentage = (totalCompletedInTime.toDouble() / totalReferrals) * 100

    return String.format("%.1f%%", percentage)
  }
}
