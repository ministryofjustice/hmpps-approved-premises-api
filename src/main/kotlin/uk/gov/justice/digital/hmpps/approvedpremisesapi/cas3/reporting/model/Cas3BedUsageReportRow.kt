package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import java.time.LocalDate

data class Cas3BedUsageReportRow(
  val probationRegion: String?,
  val pdu: String?,
  val localAuthority: String?,
  val propertyRef: String,
  val addressLine1: String,
  val town: String?,
  val postCode: String,
  val bedspaceRef: String,
  val crn: String?,
  val type: Cas3BedUsageType,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val durationOfBookingDays: Int,
  val bookingStatus: Cas3BookingStatus?,
  val overstay: String?,
  val authorised: String?,
  val reason: String?,
  val voidCategory: String?,
  val voidNotes: String?,
  val costCentre: Cas3CostCentre?,
  val uniquePropertyRef: String,
  val uniqueBedspaceRef: String,
)

enum class Cas3BedUsageType {
  Booking,
  Turnaround,
  Void,
}
