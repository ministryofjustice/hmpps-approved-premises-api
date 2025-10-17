package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class BookingGapReportData(
  val bedspaces: List<BedspaceInfo>,
  val bookings: List<BookingRecord>,
  val bedspaceVoids: List<BedspaceVoid>,
)

data class BedspaceInfo(
  val id: UUID,
  val premisesName: String,
  val roomName: String,
  val probationRegion: String,
  val pduName: String,
  val startDate: LocalDate,
  val endDate: LocalDate?,
)

data class BookingRecord(
  val bedId: UUID,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  val turnaroundDays: Int?,
)

data class BedspaceBooking(
  val bedId: UUID,
  val premisesName: String,
  val roomName: String,
  val probationRegion: String,
  val pduName: String,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  val turnaroundDays: Int?,
)

data class BedspaceVoid(
  val bedId: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
)

data class UnavailablePeriod(
  val bedId: UUID,
  val startDate: LocalDate,
  val endDate: LocalDate,
) {
  // Add helper: periods are contiguous if day-adjacent
  fun isDayAdjacentWith(other: UnavailablePeriod): Boolean = endDate.plusDays(1) == other.startDate || other.endDate.plusDays(1) == startDate

  // Merge if overlap or day adjacency, and same bed
  fun canMergeWith(other: UnavailablePeriod): Boolean = bedId == other.bedId && isDayAdjacentWith(other)
}

data class DateRange(
  val startDate: LocalDate,
  val endDate: LocalDate,
) {
  fun daysBetween(): Long = ChronoUnit.DAYS.between(startDate, endDate) + 1

  override fun toString(): String = "[$startDate,$endDate]"
}

data class BedspaceUnavailableRanges(
  val bedId: UUID,
  val probationRegion: String,
  val pduName: String,
  val premisesName: String,
  val roomName: String,
  val unavailablePeriods: List<UnavailablePeriod>,
  val dateMin: LocalDate?,
  val dateMax: LocalDate?,
) {

  fun getMergedPeriods(): List<UnavailablePeriod> {
    if (unavailablePeriods.isEmpty()) return emptyList()

    val sorted = unavailablePeriods.sortedBy { it.startDate }
    val merged = mutableListOf<UnavailablePeriod>()
    var current = sorted.first()

    sorted.drop(1).forEach { period ->
      if (current.canMergeWith(period)) {
        // Merge overlapping or adjacent periods
        current = current.copy(
          endDate = maxOf(current.endDate, period.endDate),
        )
      } else {
        // No overlap - add current to result and start new period
        merged.add(current)
        current = period
      }
    }

    merged.add(current)
    return merged
  }
}
