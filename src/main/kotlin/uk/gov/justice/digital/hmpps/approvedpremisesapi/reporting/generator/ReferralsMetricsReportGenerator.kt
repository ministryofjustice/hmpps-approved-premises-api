package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApTypeCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsDataDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ReferralsMetricsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.TierCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ReferralsMetricsProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService

class ReferralsMetricsReportGenerator<T : Any>(
  private val referralsData: List<ReferralsDataDto>,
  private val workingDayService: WorkingDayService,
) : ReportGenerator<T, ReferralsMetricsReportRow, ReferralsMetricsProperties>(ReferralsMetricsReportRow::class) {
  override fun filter(properties: ReferralsMetricsProperties): (T) -> Boolean = {
    true
  }

  override val convert: T.(properties: ReferralsMetricsProperties) -> List<ReferralsMetricsReportRow> = {
    val referrals = if (this is TierCategory) {
      when (this) {
        TierCategory.ALL -> referralsData
        TierCategory.NONE -> referralsData.filter { it.tier == null }
        else -> referralsData.filter { it.tier == this.toString() }
      }
    } else if (this is ApTypeCategory) {
      when (this) {
        ApTypeCategory.ALL -> referralsData
        ApTypeCategory.ESAP -> referralsData.filter { it.isEsapApplication == true }
        ApTypeCategory.PIPE -> referralsData.filter { it.isPipeApplication == true }
        ApTypeCategory.NORMAL -> referralsData.filter {
          it.isEsapApplication == false &&
            it.isPipeApplication == false
        }
      }
    } else {
      throw RuntimeException("Unknown Metric type - ${this::class.java}")
    }

    listOf(
      ReferralsMetricsReportRow(
        category = this.toString(),
        numberOfUniqueReferrals = referrals.size,
        referralsAccepted = referrals.filter { it.decision == AssessmentDecision.ACCEPTED.toString() }.size,
        assessmentCompletionTimeliness = getCompletionTimeliness(referrals),
        referralsWithInformationRequests = referrals.filter { it.clarificationNoteCount > 0 }.size,
        referralsRejectedAccommodationNeedOnly = referralsWithRejectionReason(referrals, "Reject, not suitable for an AP: Accommodation need only"),
        referralsRejectedNeedsCannotBeMet = referralsWithRejectionReason(referrals, "Reject, not suitable for an AP: Health / social care / disability needs cannot be met"),
        referralsRejectedSupervisionPeriodTooShort = referralsWithRejectionReason(referrals, "Reject, not suitable for an AP: Remaining supervision period too short"),
        referralsRejectedRiskTooLow = referralsWithRejectionReason(referrals, "Reject, not suitable for an AP: Risk too low"),
        referralsRejectedOtherReasons = referralsWithRejectionReason(referrals, "Reject, not suitable for an AP: Not suitable for other reasons"),
        referralsRejectedRequestedInformationNotProvided = referralsWithRejectionReason(referrals, "Reject, insufficient information: Requested information not provided by probation practitioner"),
        referralsRejectedInsufficientContingencyPlan = referralsWithRejectionReason(referrals, "Reject, insufficient information: Insufficient contingency plan"),
        referralsRejectedInsufficientMoveOnPlan = referralsWithRejectionReason(referrals, "Reject, insufficient information: Insufficient move on plan"),
        referralsRejectedRiskTooHighToCommunity = referralsWithRejectionReason(referrals, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to community"),
        referralsRejectedRiskTooHighToOtherPeopleInAP = referralsWithRejectionReason(referrals, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to other people in AP"),
        referralsRejectedRiskToHighToStaff = referralsWithRejectionReason(referrals, "Reject, risk too high (must be approved by an AP Area Manager (APAM): Risk to staff"),
        referralsWithdrawn = referralsWithRejectionReason(referrals, "Application withdrawn: Application withdrawn by the probation practitioner"),
        referralsByReleaseTypeLicence = referralsWithReleaseType(referrals, "licence"),
        referralsByReleaseTypeHdc = referralsWithReleaseType(referrals, "hdc"),
        referralsByReleaseTypePSS = referralsWithReleaseType(referrals, "pss"),
        referralsByReleaseTypeParole = referralsWithReleaseType(referrals, "rotl"),
      ),
    )
  }

  private fun referralsWithRejectionReason(referrals: List<ReferralsDataDto>, rejectionReason: String) = referrals.filter {
    it.rejectionRationale == rejectionReason
  }.size

  private fun referralsWithReleaseType(referrals: List<ReferralsDataDto>, releaseType: String) = referrals.filter {
    it.releaseType == releaseType
  }.size

  private fun getCompletionTimeliness(referrals: List<ReferralsDataDto>): String {
    val totalReferrals = referrals.size

    if (totalReferrals == 0) {
      return "N/A"
    }

    val totalCompletedInTime = referrals.filter {
      val startDate = it.applicationSubmittedAt
      val endDate = it.assessmentSubmittedAt

      (startDate !== null && endDate !== null && workingDayService.getWorkingDaysCount(startDate, endDate) <= 10)
    }.size

    val percentage = (totalCompletedInTime.toDouble() / totalReferrals) * 100

    return String.format("%.1f%%", percentage)
  }
}
