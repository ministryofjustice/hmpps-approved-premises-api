package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportData

fun List<BookingEntity>.toBookingsReportData(): List<BookingsReportData> =
  this.toBookingsReportData { PersonSummaryInfoResult.Unknown(it) }

fun List<BookingEntity>.toBookingsReportData(
  configuration: (crn: String) -> PersonSummaryInfoResult,
): List<BookingsReportData> =
  this.map { BookingsReportData(it, configuration(it.crn)) }
