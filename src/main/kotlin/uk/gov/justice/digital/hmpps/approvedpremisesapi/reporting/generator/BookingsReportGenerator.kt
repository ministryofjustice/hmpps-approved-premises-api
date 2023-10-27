package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BookingsReportGenerator : ReportGenerator<BookingsReportDataAndPersonInfo, BookingsReportRow, BookingsReportProperties>(BookingsReportRow::class) {

  override val convert: BookingsReportDataAndPersonInfo.(properties: BookingsReportProperties) -> List<BookingsReportRow> = {
    val booking = this.bookingsReportData
    val personInfo = this.personInfoResult

    listOf(
      BookingsReportRow(
        bookingId = booking.bookingId,
        referralId = booking.referralId,
        referralDate = booking.referralDate,
        personName = personInfo.tryGetDetails { "${it.firstName} ${it.surname}".trim() },
        pncNumber = personInfo.tryGetDetails { it.otherIds.pncNumber },
        gender = personInfo.tryGetDetails { it.gender },
        ethnicity = personInfo.tryGetDetails { it.offenderProfile.ethnicity },
        dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
        riskOfSeriousHarm = booking.riskOfSeriousHarm,
        sexOffender = booking.sexOffender,
        needForAccessibleProperty = booking.needForAccessibleProperty,
        historyOfArsonOffence = booking.historyOfArsonOffence,
        dutyToReferMade = booking.dutyToReferMade,
        dateDutyToReferMade = booking.dateDutyToReferMade,
        isReferralEligibleForCas3 = booking.referralEligibleForCas3,
        referralEligibilityReason = booking.referralEligibilityReason,
        probationRegion = booking.probationRegion,
        crn = booking.crn,
        offerAccepted = booking.confirmationId != null,
        isCancelled = booking.cancellationId != null,
        cancellationReason = booking.cancellationReason,
        startDate = booking.startDate,
        endDate = booking.endDate,
        actualEndDate = booking.actualEndDate?.toLocalDateTime()?.toLocalDate(),
        currentNightsStayed = if (booking.actualEndDate != null) {
          null
        } else {
          booking.startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
        },
        actualNightsStayed = if (booking.startDate == null) {
          null
        } else {
          booking.actualEndDate?.let { ChronoUnit.DAYS.between(booking.startDate, it.toLocalDateTime()?.toLocalDate()).toInt() }
        },
        accommodationOutcome = booking.accommodationOutcome,
      ),
    )
  }

  override fun filter(properties: BookingsReportProperties): (BookingsReportDataAndPersonInfo) -> Boolean = {
    true
  }

  private fun<V> PersonInfoResult.tryGetDetails(value: (OffenderDetailSummary) -> V): V? {
    return when (this) {
      is PersonInfoResult.Success.Full -> value(this.offenderDetailSummary)
      else -> null
    }
  }
}
