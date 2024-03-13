package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties

class ApplicationReportGenerator : ReportGenerator<ApplicationEntityReportRow, ApplicationReportRow, ApplicationReportProperties>(ApplicationReportRow::class) {
  override fun filter(properties: ApplicationReportProperties): (ApplicationEntityReportRow) -> Boolean = {
    true
  }

  override val convert: ApplicationEntityReportRow.(properties: ApplicationReportProperties) -> List<ApplicationReportRow> = { properties ->
    listOf(
      ApplicationReportRow(
        id = this.getId(),
        crn = this.getCrn(),
        tier = this.getTier(),
        lastAllocatedToAssessorDate = this.getLastAllocatedToAssessorDate()?.toLocalDateTime()?.toLocalDate(),
        applicationAssessedDate = this.getApplicationAssessedDate()?.toLocalDate(),
        assessorCru = this.getAssessorCru(),
        assessmentDecision = this.deriveAssessmentDecision(),
        assessmentDecisionRationale = this.getAssessmentDecisionRationale(),
        applicantReasonForLateApplication = this.getApplicantReasonForLateApplication(),
        applicantReasonForLateApplicationDetail = this.getApplicantReasonForLateApplicationDetail(),
        assessorAgreeWithShortNoticeReason = this.getAssessorAgreeWithShortNoticeReason(),
        assessorReasonForLateApplication = this.getAssessorReasonForLateApplication(),
        assessorReasonForLateApplicationDetail = this.getAssessorReasonForLateApplicationDetail(),
        ageInYears = this.getAgeInYears()?.toInt(),
        gender = this.getGender(),
        mappa = this.getMappa() ?: "Not found",
        offenceId = this.getOffenceId(),
        noms = this.getNoms(),
        premisesType = this.getPremisesType(),
        releaseType = this.getReleaseType(),
        sentenceType = this.getSentenceType(),
        applicationSubmissionDate = this.getApplicationSubmissionDate()?.toLocalDate(),
        referralLdu = this.getReferralLdu(),
        referralRegion = this.getReferralRegion(),
        referralTeam = this.getReferralTeam(),
        referrerUsername = this.getReferrerUsername(),
        targetLocation = this.getTargetLocation(),
        applicationWithdrawalReason = this.getApplicationWithdrawalReason(),
        applicationWithdrawalDate = this.getApplicationWithdrawalDate()?.toLocalDate(),
        expectedArrivalDate = this.getExpectedArrivalDate()?.toLocalDate(),
        expectedDepartureDate = this.getExpectedDepartureDate()?.toLocalDate(),
        assessmentAppealCount = this.getAssessmentAppealCount(),
        lastAssessmentAppealedDecision = this.getLastAssessmentAppealedDecision(),
        lastAssessmentAppealedDate = this.getLastAssessmentAppealedDate()?.toLocalDate(),
        assessmentAppealedFromStatus = this.getAssessmentAppealedFromStatus(),
      ),
    )
  }

  private fun ApplicationEntityReportRow.deriveAssessmentDecision() = when (this.getAssessmentAppealCount()) {
    0, null -> this.getAssessmentDecision()
    else -> this.getLastAssessmentAppealedDecision()
  }
}
