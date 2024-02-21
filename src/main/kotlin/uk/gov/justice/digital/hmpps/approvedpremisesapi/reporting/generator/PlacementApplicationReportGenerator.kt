package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntityReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.PlacementApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.PlacementApplicationReportProperties

class PlacementApplicationReportGenerator :
  ReportGenerator<PlacementApplicationEntityReportRow, PlacementApplicationReportRow, PlacementApplicationReportProperties>(PlacementApplicationReportRow::class) {

  override fun filter(properties: PlacementApplicationReportProperties): (PlacementApplicationEntityReportRow) -> Boolean = {
    true
  }

  override val convert: PlacementApplicationEntityReportRow.(properties: PlacementApplicationReportProperties) -> List<PlacementApplicationReportRow> = { properties ->
    listOf(
      PlacementApplicationReportRow(
        placementRequestId = this.getId(),
        crn = this.getCrn(),
        placementRequestSubmittedAt = this.getPlacementApplicationSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        requestedArrivalDate = this.getRequestedArrivalDate()?.toLocalDate(),
        requestedDurationDays = this.getRequestedDurationDays(),
        decision = this.getDecision(),
        decisionMadeAt = this.getDecisionMadeAt()?.toLocalDateTime()?.toLocalDate(),
        applicationSubmittedAt = this.getApplicationSubmittedAt()?.toLocalDateTime()?.toLocalDate(),
        applicationAssessedDate = this.getApplicationAssessedDate()?.toLocalDate(),
        assessorCru = this.getAssessorCru(),
        assessmentDecision = this.getAssessmentDecision(),
        assessmentDecisionRationale = this.getAssessmentDecisionRationale(),
        ageInYears = this.getAgeInYears()?.toInt(),
        gender = this.getGender(),
        mappa = this.getMappa() ?: "Not found",
        offenceId = this.getOffenceId(),
        noms = this.getNoms(),
        sentenceType = this.getSentenceType(),
        releaseType = this.getReleaseType(),
        referralLdu = this.getReferralLdu(),
        referralTeam = this.getReferralTeam(),
        referralRegion = this.getReferralRegion(),
        referrerUsername = this.getReferrerUsername(),
        targetLocation = this.getTargetLocation(),
        applicationWithdrawalDate = this.getApplicationWithdrawalDate()?.toLocalDate(),
        applicationWithdrawalReason = this.getApplicationWithdrawalReason(),
        bookingID = this.getBookingID(),
        bookingMadeDate = this.getBookingMadeDate()?.toLocalDateTime()?.toLocalDate(),
        bookingCancellationReason = this.getBookingCancellationReason(),
        bookingCancellationDate = this.getBookingCancellationDate()?.toLocalDate(),
        expectedArrivalDate = this.getExpectedArrivalDate()?.toLocalDate(),
        expectedDepartureDate = this.getExpectedDepartureDate()?.toLocalDate(),
        premisesName = this.getPremisesName(),
        actualArrivalDate = this.getActualArrivalDate()?.toLocalDate(),
        actualDepartureDate = this.getActualDepartureDate()?.toLocalDate(),
        departureReason = this.getDepartureReason(),
        departureMoveOnCategory = this.getDepartureMoveOnCategory(),
        hasNotArrived = this.getHasNotArrived(),
        notArrivedReason = this.getNotArrivedReason(),
        placementRequestType = this.getPlacementRequestType(),
        paroleDecisionDate = this.getParoleDecisionDate()?.toLocalDate(),
      ),
    )
  }
}
