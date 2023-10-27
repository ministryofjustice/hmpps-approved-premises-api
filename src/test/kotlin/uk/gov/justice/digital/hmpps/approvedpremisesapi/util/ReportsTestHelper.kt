package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportData

fun List<BookingEntity>.toBookingsReportData(): List<BookingsReportData> =
  this.toBookingsReportData { PersonInfoResult.Unknown(it) }

fun List<BookingEntity>.toBookingsReportData(configuration: (crn: String) -> PersonInfoResult): List<BookingsReportData> =
  this.map { BookingsReportData(it, configuration(it.crn)) }
