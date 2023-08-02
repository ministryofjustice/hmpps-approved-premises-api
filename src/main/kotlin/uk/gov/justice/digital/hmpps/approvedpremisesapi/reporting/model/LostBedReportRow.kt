package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate
import java.util.UUID

data class LostBedReportRow(
  val roomName: String,
  val bedName: String,
  val id: UUID,
  val workOrderId: String?,
  val region: String,
  val ap: String,
  val reason: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val lengthDays: Int,
)
