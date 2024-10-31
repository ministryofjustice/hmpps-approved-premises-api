package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties

import java.time.LocalDate

data class BookingGapReportProperties(
  val startDate: LocalDate,
  val endDate: LocalDate,
)
