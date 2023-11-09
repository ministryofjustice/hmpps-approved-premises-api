package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BookingsReportGenerator : ReportGenerator<BookingEntity, BookingsReportRow, BookingsReportProperties>(BookingsReportRow::class) {

  override val convert: BookingEntity.(properties: BookingsReportProperties) -> List<BookingsReportRow> = {
    val application = this.application as? TemporaryAccommodationApplicationEntity

    listOf(
      BookingsReportRow(
        referralId = application?.id?.toString(),
        referralDate = application?.submittedAt?.toLocalDate(),
        riskOfSeriousHarm = application?.riskRatings?.roshRisks?.value?.overallRisk,
        sexOffender = application?.isRegisteredSexOffender,
        needForAccessibleProperty = application?.needsAccessibleProperty,
        historyOfArsonOffence = application?.hasHistoryOfArson,
        dutyToReferMade = application?.isDutyToReferSubmitted,
        dateDutyToReferMade = application?.dutyToReferSubmissionDate,
        isReferralEligibleForCas3 = application?.isEligible,
        referralEligibilityReason = application?.eligibilityReason,
        probationRegion = this.premises.probationRegion.name,
        crn = this.crn,
        offerAccepted = this.confirmation != null,
        isCancelled = this.cancellation != null,
        cancellationReason = this.cancellation?.reason?.name,
        startDate = this.arrival?.arrivalDate,
        endDate = this.arrival?.expectedDepartureDate,
        actualEndDate = this.departure?.dateTime?.toLocalDate(),
        currentNightsStayed = if (this.departure != null) {
          null
        } else {
          this.arrival?.arrivalDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
        },
        actualNightsStayed = if (this.arrival?.arrivalDate == null) null else this.departure?.dateTime?.let { ChronoUnit.DAYS.between(this.arrival?.arrivalDate, it.toLocalDate()).toInt() },
        accommodationOutcome = this.departure?.moveOnCategory?.name,
      ),
    )
  }

  override fun filter(properties: BookingsReportProperties): (BookingEntity) -> Boolean = {
    it.service == properties.serviceName.value &&
      (properties.probationRegionId == null || it.premises.probationRegion.id == properties.probationRegionId)
  }
}
