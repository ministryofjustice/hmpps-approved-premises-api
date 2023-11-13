package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportData
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BookingsReportGenerator : ReportGenerator<BookingsReportData, BookingsReportRow, BookingsReportProperties>(BookingsReportRow::class) {

  override val convert: BookingsReportData.(properties: BookingsReportProperties) -> List<BookingsReportRow> = {
    listOf(
      BookingsReportRow(
        bookingId = this.bookingId,
        referralId = this.referralId,
        referralDate = this.referralDate,
        riskOfSeriousHarm = this.riskOfSeriousHarm,
        sexOffender = this.sexOffender,
        needForAccessibleProperty = this.needForAccessibleProperty,
        historyOfArsonOffence = this.historyOfArsonOffence,
        dutyToReferMade = this.dutyToReferMade,
        dateDutyToReferMade = this.dateDutyToReferMade,
        isReferralEligibleForCas3 = this.referralEligibleForCas3,
        referralEligibilityReason = this.referralEligibilityReason,
        probationRegion = this.probationRegion,
        crn = this.crn,
        offerAccepted = this.confirmationId != null,
        isCancelled = this.cancellationId != null,
        cancellationReason = this.cancellationReason,
        startDate = this.startDate,
        endDate = this.endDate,
        actualEndDate = this.actualEndDate?.toLocalDateTime()?.toLocalDate(),
        currentNightsStayed = if (this.actualEndDate != null) {
          null
        } else {
          this.startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
        },
        actualNightsStayed = if (this.startDate == null) {
          null
        } else {
          this.actualEndDate?.let { ChronoUnit.DAYS.between(this.startDate, it.toLocalDateTime()?.toLocalDate()).toInt() }
        },
        accommodationOutcome = this.accommodationOutcome,
      ),
    )
  }

  override fun filter(properties: BookingsReportProperties): (BookingsReportData) -> Boolean = {
    true
  }
}
