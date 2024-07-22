package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.toYesNo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
        personName = personInfo.tryGetDetails {
          val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
          nameParts.joinToString(" ")
        },
        pncNumber = personInfo.tryGetDetails { it.pnc },
        gender = personInfo.tryGetDetails { it.gender },
        ethnicity = personInfo.tryGetDetails { it.profile?.ethnicity },
        dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
        riskOfSeriousHarm = booking.riskOfSeriousHarm,
        registeredSexOffender = booking.registeredSexOffender.toYesNo(),
        historyOfSexualOffence = booking.historyOfSexualOffence.toYesNo(),
        concerningSexualBehaviour = booking.concerningSexualBehaviour.toYesNo(),
        needForAccessibleProperty = booking.needForAccessibleProperty.toYesNo(),
        historyOfArsonOffence = booking.historyOfArsonOffence.toYesNo(),
        concerningArsonBehaviour = booking.concerningArsonBehaviour.toYesNo(),
        dutyToReferMade = booking.dutyToReferMade.toYesNo(),
        dateDutyToReferMade = booking.dateDutyToReferMade,
        dutyToReferLocalAuthorityAreaName = booking.dutyToReferLocalAuthorityAreaName,
        isReferralEligibleForCas3 = booking.referralEligibleForCas3.toYesNo(),
        referralEligibilityReason = booking.referralEligibilityReason,
        probationRegion = booking.probationRegionName,
        pdu = booking.pdu,
        localAuthority = booking.localAuthorityAreaName,
        town = booking.town,
        postCode = booking.postCode,
        crn = booking.crn,
        offerAccepted = (booking.confirmationId != null).toYesNo()!!,
        isCancelled = (booking.cancellationId != null).toYesNo()!!,
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
          booking.actualEndDate?.let { ChronoUnit.DAYS.between(LocalDateTime.of(booking.startDate, LocalTime.MAX), it.toLocalDateTime()).toInt() + 1 }
        },
        accommodationOutcome = booking.accommodationOutcome,
      ),
    )
  }

  override fun filter(properties: BookingsReportProperties): (BookingsReportDataAndPersonInfo) -> Boolean = {
    true
  }

  private fun<V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
    return when (this) {
      is PersonSummaryInfoResult.Success.Full -> value(this.summary)
      else -> null
    }
  }
}
