package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class BookingsReportRow(
  val probationRegion: String,
  val crn: String,
  val offerAccepted: Boolean,
  val isCancelled: Boolean,
  val cancellationReason: String?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val actualEndDate: LocalDate?,
  val currentNightsStayed: Int?,
  val actualNightsStayed: Int?,
  val accommodationOutcome: String?,
)
