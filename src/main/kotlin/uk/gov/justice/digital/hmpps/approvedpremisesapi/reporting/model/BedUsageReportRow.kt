package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import java.time.LocalDate

data class BedUsageReportRow(
  val pdu: String?,
  val propertyRef: String,
  val addressLine1: String,
  val bedspaceRef: String,
  val crn: String?,
  val type: BedUsageType,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val durationOfBookingDays: Int,
  val bookingStatus: BookingStatus?,
  val voidCategory: String?,
  val voidNotes: String?
)

enum class BedUsageType {
  Booking,
  Turnaround,
  Void
}
