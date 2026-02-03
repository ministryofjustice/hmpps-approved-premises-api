package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.generator

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.util.toYesNo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.MAX_DAYS_STAY
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.String

class BookingsReportGenerator : ReportGenerator<BookingsReportDataAndPersonInfo, BookingsReportRow, BookingsReportProperties>(BookingsReportRow::class) {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val convert: BookingsReportDataAndPersonInfo.(properties: BookingsReportProperties) -> List<BookingsReportRow> = {
    val booking = this.bookingsReportData
    val personInfo = this.personInfoReportData

    val actualNightsStayed = actualNightsStayed(booking)
    val (overstay, authorised, reason) = getOverstayData(actualNightsStayed, booking)

    listOf(
      BookingsReportRow(
        bookingId = booking.bookingId,
        referralId = booking.referralId,
        referralDate = booking.referralDate?.toLocalDate(),
        personName = personInfo.name?.let { (listOf(it.forename) + it.middleNames + it.surname) }?.joinToString(" "),
        pncNumber = personInfo.pnc,
        gender = personInfo.gender,
        ethnicity = personInfo.ethnicity,
        dateOfBirth = personInfo.dateOfBirth,
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
        actualEndDate = booking.actualEndDate?.toLocalDate(),
        currentNightsStayed = if (booking.actualEndDate != null) {
          null
        } else {
          booking.startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() }
        },
        actualNightsStayed = actualNightsStayed,
        overstay = overstay,
        authorised = authorised,
        reason = reason,
        accommodationOutcome = booking.accommodationOutcome,
      ),
    )
  }

  private fun getOverstayData(
    actualNightsStayed: Int?,
    booking: BookingsReportData,
  ): Triple<String?, String?, String?> {
    if (actualNightsStayed != null && actualNightsStayed > MAX_DAYS_STAY) {
      if (booking.overstayCreatedAt == null || booking.overstayIsAuthorised == null) {
        log.warn("booking ${booking.bookingId} is over ${MAX_DAYS_STAY} but has no overstay record")
      }
      val authorised = if (booking.overstayIsAuthorised != null && booking.overstayIsAuthorised!!) "Y" else "N"
      val reason = booking.overstayReason
      return Triple("Y", authorised, reason)
    }
    return Triple("N", null, null)
  }

  private fun actualNightsStayed(booking: BookingsReportData): Int? {
    val actualNightsStayed = if (booking.startDate == null) {
      null
    } else {
      booking.actualEndDate?.let {
        ChronoUnit.DAYS.between(
          LocalDateTime.of(booking.startDate, LocalTime.MAX),
          it.toLocalDateTime(),
        ).toInt() + 1
      }
    }
    return actualNightsStayed
  }

  override fun filter(properties: BookingsReportProperties): (BookingsReportDataAndPersonInfo) -> Boolean = {
    true
  }
}
