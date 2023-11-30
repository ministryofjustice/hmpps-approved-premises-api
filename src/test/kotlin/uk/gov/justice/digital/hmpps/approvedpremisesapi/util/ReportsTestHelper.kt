package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportData
import java.sql.Timestamp
import java.time.LocalDate

fun List<BookingEntity>.toBookingsReportData(): List<BookingsReportData> = this
  .map {
    val application = it.application as? TemporaryAccommodationApplicationEntity
    object : BookingsReportData {
      override val bookingId: String
        get() = it.id.toString()
      override val referralId: String?
        get() = application?.id?.toString()
      override val referralDate: LocalDate?
        get() = application?.submittedAt?.toLocalDate()
      override val riskOfSeriousHarm: String?
        get() = application?.riskRatings?.roshRisks?.value?.overallRisk
      override val sexOffender: Boolean?
        get() = application?.isRegisteredSexOffender
      override val needForAccessibleProperty: Boolean?
        get() = application?.needsAccessibleProperty
      override val historyOfArsonOffence: Boolean?
        get() = application?.hasHistoryOfArson
      override val dutyToReferMade: Boolean?
        get() = application?.isDutyToReferSubmitted
      override val dateDutyToReferMade: LocalDate?
        get() = application?.dutyToReferSubmissionDate
      override val referralEligibleForCas3: Boolean?
        get() = application?.isEligible
      override val referralEligibilityReason: String?
        get() = application?.eligibilityReason
      override val probationRegionName: String
        get() = it.premises.probationRegion.name
      override val localAuthorityAreaName: String?
        get() = it.premises.localAuthorityArea?.name
      override val crn: String
        get() = it.crn
      override val confirmationId: String?
        get() = it.confirmation?.id?.toString()
      override val cancellationId: String?
        get() = it.cancellation?.id?.toString()
      override val cancellationReason: String?
        get() = it.cancellation?.reason?.name
      override val startDate: LocalDate?
        get() = it.arrival?.arrivalDate
      override val endDate: LocalDate?
        get() = it.arrival?.expectedDepartureDate
      override val actualEndDate: Timestamp?
        get() = it.departure?.dateTime?.let { time -> Timestamp.from(time.toInstant()) }
      override val accommodationOutcome: String?
        get() = it.departure?.moveOnCategory?.name
      override val dutyToReferLocalAuthorityAreaName: String?
        get() = parseAndGetDutyToReferLocalAuthorityAreaName(application)
    }
  }
  .sortedBy { it.bookingId }

fun List<BookingEntity>.toBookingsReportDataAndPersonInfo(): List<BookingsReportDataAndPersonInfo> =
  this.toBookingsReportDataAndPersonInfo { PersonSummaryInfoResult.Unknown(it) }

fun List<BookingEntity>.toBookingsReportDataAndPersonInfo(configuration: (crn: String) -> PersonSummaryInfoResult): List<BookingsReportDataAndPersonInfo> =
  this.toBookingsReportData().map { BookingsReportDataAndPersonInfo(it, configuration(it.crn)) }

private fun parseAndGetDutyToReferLocalAuthorityAreaName(application: TemporaryAccommodationApplicationEntity?): String? =
  if (!application?.data.isNullOrEmpty()) {
    val config = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS)
    JsonPath.using(config).parse(application?.data).read<String>(buildString { append(ACCOMMODATION_REFERRAL_DETAILS_DTR_DETAILS_LOCAL_AUTHORITY_AREA_NAME_PATH) })
  } else {
    null
  }

private const val ACCOMMODATION_REFERRAL_DETAILS_DTR_DETAILS_LOCAL_AUTHORITY_AREA_NAME_PATH = "$['accommodation-referral-details']['dtr-details']['localAuthorityAreaName']"
