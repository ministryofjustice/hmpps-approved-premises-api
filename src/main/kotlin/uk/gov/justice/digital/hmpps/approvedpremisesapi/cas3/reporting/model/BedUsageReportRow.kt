package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import java.time.LocalDate

data class BedUsageReportRow(
  val probationRegion: String?,
  val pdu: String?,
  val localAuthority: String?,
  val propertyRef: String,
  val addressLine1: String,
  val town: String?,
  val postCode: String,
  val bedspaceRef: String,
  val crn: String?,
  val type: BedUsageType,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val durationOfBookingDays: Int,
  val bookingStatus: BookingStatus?,
  val voidCategory: String?,
  val voidNotes: String?,
  val costCentre: Cas3CostCentre?,
  val uniquePropertyRef: String,
  val uniqueBedspaceRef: String,
)

enum class BedUsageType {
  Booking,
  Turnaround,
  Void,
}
