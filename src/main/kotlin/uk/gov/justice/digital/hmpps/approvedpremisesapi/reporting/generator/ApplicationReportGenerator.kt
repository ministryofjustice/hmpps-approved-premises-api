package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import kotlinx.datetime.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntityReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.ApplicationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import java.time.LocalDate
import java.time.Period

class ApplicationReportGenerator(
  private val offenderService: OffenderService,
) : ReportGenerator<ApplicationEntityReportRow, ApplicationReportRow, ApplicationReportProperties>(ApplicationReportRow::class) {
  override fun filter(properties: ApplicationReportProperties): (ApplicationEntityReportRow) -> Boolean = {
    true
  }

  override val convert: ApplicationEntityReportRow.(properties: ApplicationReportProperties) -> List<ApplicationReportRow> = { properties ->
    val personInfoResult = getOffenderDetailForApplication(this, properties.deliusUsername)

    listOf(
      ApplicationReportRow(
        id = this.getId(),
        crn = this.getCrn(),
        applicationAssessedDate = this.getApplicationAssessedDate()?.toLocalDateTime()?.toLocalDate(),
        assessorCru = this.getAssessorCru(),
        assessmentDecision = this.getAssessmentDecision(),
        assessmentDecisionRationale = this.getAssessmentDecisionRationale(),
        ageInYears = when (personInfoResult) {
          is PersonInfoResult.Success.Full -> Period.between(personInfoResult.offenderDetailSummary.dateOfBirth, LocalDate.now()).years
          else -> null
        },
        gender = when (personInfoResult) {
          is PersonInfoResult.Success.Full -> personInfoResult.offenderDetailSummary.gender
          else -> null
        },
        mappa = this.getMappa() ?: "Not found",
        offenceId = this.getOffenceId(),
        noms = this.getNoms(),
        premisesType = this.getPremisesType(),
        releaseType = this.getReleaseType(),
        sentenceLengthInMonths = null,
        applicationSubmissionDate = this.getApplicationSubmissionDate()?.toLocalDateTime()?.toLocalDate(),
        referrerLdu = null,
        referrerRegion = this.getReferrerRegion(),
        referrerTeam = null,
        targetLocation = this.getTargetLocation(),
        applicationWithdrawalReason = this.getApplicationWithdrawalReason(),
        applicationWithdrawalDate = null,
        bookingID = this.getBookingID(),
        bookingCancellationReason = this.getBookingCancellationReason(),
        bookingCancellationDate = this.getBookingCancellationDate()?.toLocalDate(),
        expectedArrivalDate = this.getExpectedArrivalDate()?.toLocalDate(),
        matcherCru = null,
        expectedDepartureDate = this.getExpectedDepartureDate()?.toLocalDate(),
        premisesName = this.getPremisesName(),
        actualArrivalDate = this.getActualArrivalDate()?.toLocalDate(),
        actualDepartureDate = this.getActualDepartureDate()?.toLocalDateTime()?.toLocalDate(),
        departureMoveOnCategory = this.getDepartureMoveOnCategory(),
        nonArrivalDate = this.getNonArrivalDate()?.toLocalDate(),
      ),
    )
  }

  private fun getOffenderDetailForApplication(applicationEntityReportRow: ApplicationEntityReportRow, deliusUsername: String): PersonInfoResult? {
    return when (val personInfo = offenderService.getInfoForPerson(applicationEntityReportRow.getCrn(), deliusUsername, true)) {
      is PersonInfoResult.Success -> personInfo
      else -> null
    }
  }
}
