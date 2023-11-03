package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BookingsReportGenerator : ReportGenerator<BookingsReportData, BookingsReportRow, BookingsReportProperties>(BookingsReportRow::class) {

  override val convert: BookingsReportData.(properties: BookingsReportProperties) -> List<BookingsReportRow> = {
    val booking = this.booking
    val application = booking.application as? TemporaryAccommodationApplicationEntity
    val personInfo = this.personInfoResult

    listOf(
      BookingsReportRow(
        referralId = application?.id?.toString(),
        referralDate = application?.submittedAt?.toLocalDate(),
        personName = personInfo.tryGetDetails {
          val nameParts = listOf(it.name.forename) + it.name.middleNames + it.name.surname
          nameParts.joinToString(" ")
        },
        pncNumber = null,
        gender = personInfo.tryGetDetails { it.gender },
        ethnicity = personInfo.tryGetDetails { it.profile?.ethnicity },
        dateOfBirth = personInfo.tryGetDetails { it.dateOfBirth },
        riskOfSeriousHarm = application?.riskRatings?.roshRisks?.value?.overallRisk,
        sexOffender = application?.isRegisteredSexOffender,
        needForAccessibleProperty = application?.needsAccessibleProperty,
        historyOfArsonOffence = application?.hasHistoryOfArson,
        dutyToReferMade = application?.isDutyToReferSubmitted,
        dateDutyToReferMade = application?.dutyToReferSubmissionDate,
        isReferralEligibleForCas3 = application?.isEligible,
        referralEligibilityReason = application?.eligibilityReason,
        probationRegion = booking.premises.probationRegion.name,
        crn = booking.crn,
        offerAccepted = booking.confirmation != null,
        isCancelled = booking.cancellation != null,
        cancellationReason = booking.cancellation?.reason?.name,
        startDate = booking.arrival?.arrivalDate,
        endDate = booking.arrival?.expectedDepartureDate,
        actualEndDate = booking.departure?.dateTime?.toLocalDate(),
        currentNightsStayed = if (booking.departure != null) {
          null
        } else {
          booking.arrival?.arrivalDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
        },
        actualNightsStayed = if (booking.arrival?.arrivalDate == null) {
          null
        } else {
          booking.departure
            ?.dateTime
            ?.let { ChronoUnit.DAYS.between(booking.arrival?.arrivalDate, it.toLocalDate()).toInt() }
        },
        accommodationOutcome = booking.departure?.moveOnCategory?.name,
      ),
    )
  }

  override fun filter(properties: BookingsReportProperties): (BookingsReportData) -> Boolean = {
    it.booking.service == properties.serviceName.value &&
      (properties.probationRegionId == null || it.booking.premises.probationRegion.id == properties.probationRegionId)
  }

  private fun<V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
    return when (this) {
      is PersonSummaryInfoResult.Success.Full -> value(this.summary)
      else -> null
    }
  }
}
