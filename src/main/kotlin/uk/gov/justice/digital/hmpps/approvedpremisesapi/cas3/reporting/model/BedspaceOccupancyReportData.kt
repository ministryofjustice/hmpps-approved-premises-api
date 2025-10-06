package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model

import java.time.Instant
import java.time.LocalDate

data class BedspaceOccupancyReportData(
  val bedspaceReportData: BedspaceOccupancyBedspaceReportData,
  val bookingsReportData: List<BedspaceOccupancyBookingReportData>,
  val bookingCancellationReportData: List<BedspaceOccupancyBookingCancellationReportData>,
  val bookingTurnaroundReportData: List<BedspaceOccupancyBookingTurnaroundReportData>,
  val voidBedspaceReportData: List<BedspaceOccupancyVoidBedspaceReportData>,
)

interface BedspaceOccupancyBedspaceReportData {
  val bedspaceId: String
  val probationRegionName: String?
  val probationDeliveryUnitName: String?
  val localAuthorityName: String?
  val premisesName: String
  val addressLine1: String
  val town: String?
  val postCode: String
  val roomName: String
  val bedspaceStartDate: Instant?
  val bedspaceEndDate: LocalDate?
  val premisesId: String
}

interface BedspaceOccupancyBookingReportData {
  val bookingId: String
  val arrivalDate: LocalDate
  val departureDate: LocalDate
  val bedspaceId: String
  val arrivalId: String?
  val arrivalCreatedAt: Instant?
  val confirmationId: String?
}

interface BedspaceOccupancyBookingCancellationReportData {
  val cancellationId: String
  val bedspaceId: String
  val bookingId: String
  val createdAt: Instant
}

interface BedspaceOccupancyBookingTurnaroundReportData {
  val turnaroundId: String
  val bedspaceId: String
  val bookingId: String
  val workingDayCount: Int
  val createdAt: Instant
}

interface BedspaceOccupancyVoidBedspaceReportData {
  val bedspaceId: String
  val startDate: LocalDate
  val endDate: LocalDate
  val cancellationId: String?
}
