package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class VoidBedspaceReportRow(
  val roomName: String,
  val bedName: String,
  val id: String,
  val workOrderId: String?,
  val region: String,
  val ap: String,
  val reason: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val lengthDays: Int,
)
