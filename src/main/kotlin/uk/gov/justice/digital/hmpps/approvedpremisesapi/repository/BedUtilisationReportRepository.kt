package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface BedUtilisationReportRepository : JpaRepository<BedEntity, UUID> {
  @Query(
    """
    SELECT
        CAST(b.id AS VARCHAR) AS bedId,
        CAST(r.id AS VARCHAR) AS roomId,
        CAST(p.id AS VARCHAR) AS premisesId,
        pr.name AS probationRegionName,
        pdu.name AS probationDeliveryUnitName,
        laa.name AS localAuthorityName,
        p.name AS premisesName,
        p.address_line1 AS addressLine1,
        p.town AS town,
        p.postcode AS postCode,
        r.name AS roomName,
        b.created_at AS bedspaceStartDate,
        b.end_date AS bedspaceEndDate
    FROM beds b
    INNER JOIN rooms r ON b.room_id = r.id
    LEFT JOIN premises p ON r.premises_id = p.id
    LEFT JOIN probation_regions pr ON p.probation_region_id = pr.id
    INNER JOIN temporary_accommodation_premises tap ON p.id = tap.premises_id
    LEFT JOIN probation_delivery_units pdu ON tap.probation_delivery_unit_id = pdu.id
    LEFT JOIN local_authority_areas laa ON p.local_authority_area_id = laa.id
    WHERE
      p.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR p.probation_region_id = :probationRegionId)
    ORDER BY b.name      
    """,
    nativeQuery = true,
  )
  fun findAllBedspaces(
    probationRegionId: UUID?,
  ): List<BedUtilisationBedspaceReportData>

  @Query(
    """
    SELECT
      CAST(booking.id AS VARCHAR) AS bookingId,
      booking.arrival_date AS arrivalDate,
      booking.departure_date AS departureDate,
      CAST(bed.id AS VARCHAR) AS bedId,
      CAST(arrival.id AS VARCHAR) AS arrivalId,
      arrival.created_at AS arrivalCreatedAt,
      CAST(confirmation.id AS VARCHAR) AS confirmationId
    From bookings booking
    INNER JOIN beds bed ON bed.id = booking.bed_id
    INNER JOIN premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_regions probation_region ON probation_region.id = premises.probation_region_id
    LEFT JOIN arrivals arrival ON booking.id = arrival.booking_id
    LEFT JOIN confirmations confirmation ON booking.id = confirmation.booking_id
    WHERE
        premises.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR premises.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingsByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedUtilisationBookingReportData>

  @Query(
    """
    SELECT
      CAST(cancellation.id AS VARCHAR) AS cancellationId,
      CAST(bed.id AS VARCHAR) AS bedId,
      CAST(booking.id AS VARCHAR) AS bookingId,
      cancellation.created_at AS createdAt
    From cancellations cancellation
    INNER JOIN bookings booking ON cancellation.booking_id = booking.id
    INNER JOIN beds bed ON bed.id = booking.bed_id
    INNER JOIN premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_regions probation_region ON probation_region.id = premises.probation_region_id
    WHERE
        premises.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR premises.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingCancellationsByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedUtilisationBookingCancellationReportData>

  @Query(
    """
    SELECT
      CAST(turnaround.Id AS VARCHAR) AS turnaroundId,
      CAST(bed.id AS VARCHAR) AS bedId,
      CAST(booking.id AS VARCHAR) AS bookingId,
      turnaround.created_at AS CreatedAt,
      working_day_count AS workingDayCount
    From turnarounds turnaround
    INNER JOIN bookings booking ON booking.id = turnaround.booking_id
    INNER JOIN beds bed ON bed.id = booking.bed_id
    INNER JOIN premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_regions probation_region ON probation_region.id = premises.probation_region_id
    WHERE
        premises.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR premises.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingTurnaroundByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedUtilisationBookingTurnaroundReportData>

  @Query(
    """
    SELECT
        CAST(b.id AS VARCHAR) AS bedId,
        vb.start_date AS startDate,
        vb.end_date AS endDate,
        CAST(vbc.id AS VARCHAR) AS cancellationId
    From cas3_void_bedspaces vb
    LEFT JOIN beds b ON vb.bed_id = b.id
    LEFT JOIN rooms r ON b.room_id = r.id
    LEFT JOIN premises p ON r.premises_id = p.id
    LEFT JOIN cas3_void_bedspace_cancellations vbc ON vb.id = vbc.lost_bed_id
    WHERE
        p.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR p.probation_region_id = :probationRegionId)
      AND vb.start_date <= :endDate AND vb.end_date >= :startDate
      AND vbc.id is NULL
    ORDER BY vb.id     
    """,
    nativeQuery = true,
  )
  fun findAllVoidBedspaceByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedUtilisationVoidBedspaceReportData>
}
interface BedUtilisationBedspaceReportData {
  val bedId: String
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
  val roomId: String
}

interface BedUtilisationBookingReportData {
  val bookingId: String
  val arrivalDate: LocalDate
  val departureDate: LocalDate
  val bedId: String
  val arrivalId: String?
  val arrivalCreatedAt: Instant?
  val confirmationId: String?
}

interface BedUtilisationBookingCancellationReportData {
  val cancellationId: String
  val bedId: String
  val bookingId: String
  val createdAt: Instant
}

interface BedUtilisationBookingTurnaroundReportData {
  val turnaroundId: String
  val bedId: String
  val bookingId: String
  val workingDayCount: Int
  val createdAt: Instant
}

interface BedUtilisationVoidBedspaceReportData {
  val bedId: String
  val startDate: LocalDate
  val endDate: LocalDate
  val cancellationId: String?
}
