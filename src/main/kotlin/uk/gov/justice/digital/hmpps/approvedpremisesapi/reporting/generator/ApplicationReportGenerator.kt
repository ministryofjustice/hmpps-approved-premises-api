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
        lastAllocatedToAssessorDate = this.getLastAllocatedToAssessorDate()?.toLocalDateTime()?.toLocalDate(),
        applicationAssessedDate = this.getApplicationAssessedDate()?.toLocalDate(),
        assessorCru = this.getAssessorCru(),
        assessmentDecision = this.getAssessmentDecision(),
        assessmentDecisionRationale = this.getAssessmentDecisionRationale(),
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
        bookingID = this.getBookingID(),
        bookingMadeDate = this.getBookingMadeDate()?.toLocalDateTime()?.toLocalDate(),
        bookingCancellationReason = this.getBookingCancellationReason(),
        bookingCancellationDate = this.getBookingCancellationDate()?.toLocalDate(),
        expectedArrivalDate = this.getExpectedArrivalDate()?.toLocalDate(),
        matcherCru = this.getMatcherCru(),
        expectedDepartureDate = this.getExpectedDepartureDate()?.toLocalDate(),
        premisesName = this.getPremisesName(),
        actualArrivalDate = this.getActualArrivalDate()?.toLocalDate(),
        actualDepartureDate = this.getActualDepartureDate()?.toLocalDate(),
        departureMoveOnCategory = this.getDepartureMoveOnCategory(),
        departureReason = this.getDepartureReason(),
        hasNotArrived = this.getHasNotArrived(),
        notArrivedReason = this.getNotArrivedReason(),
        paroleDecisionDate = this.getParoleDecisionDate()?.toLocalDate(),
        type = this.getType(),
      ),
    )
  }
}
