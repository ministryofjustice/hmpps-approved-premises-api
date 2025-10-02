package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBookingCancellationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBookingReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyBookingTurnaroundReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceOccupancyVoidBedspaceReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import java.time.LocalDate
import java.util.UUID

interface BedspaceOccupancyReportRepository : JpaRepository<BedEntity, UUID> {
  @Query(
    """
    SELECT
        CAST(b.id AS VARCHAR) AS bedspaceId,
        CAST(p.id AS VARCHAR) AS premisesId,
        pr.name AS probationRegionName,
        pdu.name AS probationDeliveryUnitName,
        laa.name AS localAuthorityName,
        p.name AS premisesName,
        p.address_line1 AS addressLine1,
        p.town AS town,
        p.postcode AS postCode,
        b.reference AS roomName,
        b.created_at AS bedspaceStartDate,
        b.end_date AS bedspaceEndDate
    FROM cas3_bedspaces b
    LEFT JOIN cas3_premises p ON b.premises_id = p.id
    LEFT JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
    LEFT JOIN probation_regions pr ON pdu.probation_region_id = pr.id
    LEFT JOIN local_authority_areas laa ON p.local_authority_area_id = laa.id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
      AND ((b.created_at IS NULL OR b.created_at <= :endDate) AND (b.end_date IS NULL OR b.end_date >= :startDate))
    ORDER BY b.reference
    """,
    nativeQuery = true,
  )
  fun findAllBedspaces(
    probationRegionId: UUID?,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<BedspaceOccupancyBedspaceReportData>

  @Query(
    """
    SELECT
      CAST(booking.id AS VARCHAR) AS bookingId,
      booking.arrival_date AS arrivalDate,
      booking.departure_date AS departureDate,
      CAST(bedspace.id AS VARCHAR) AS bedspaceId,
      CAST(arrival.id AS VARCHAR) AS arrivalId,
      arrival.created_at AS arrivalCreatedAt,
      CAST(confirmation.id AS VARCHAR) AS confirmationId
    From bookings booking
    INNER JOIN cas3_bedspaces bedspace ON bedspace.id = booking.bed_id
    INNER JOIN cas3_premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_delivery_units pdu ON premises.probation_delivery_unit_id = pdu.id
    INNER JOIN probation_regions probation_region ON probation_region.id = pdu.probation_region_id
    LEFT JOIN arrivals arrival ON booking.id = arrival.booking_id
    LEFT JOIN cas3_confirmations confirmation ON booking.id = confirmation.booking_id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingsByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedspaceOccupancyBookingReportData>

  @Query(
    """
    SELECT
      CAST(cancellation.id AS VARCHAR) AS cancellationId,
      CAST(bedspace.id AS VARCHAR) AS bedspaceId,
      CAST(booking.id AS VARCHAR) AS bookingId,
      cancellation.created_at AS createdAt
    From cancellations cancellation
    INNER JOIN bookings booking ON cancellation.booking_id = booking.id
    INNER JOIN cas3_bedspaces bedspace ON bedspace.id = booking.bed_id
    INNER JOIN cas3_premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_delivery_units pdu ON premises.probation_delivery_unit_id = pdu.id
    INNER JOIN probation_regions probation_region ON probation_region.id = pdu.probation_region_id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingCancellationsByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedspaceOccupancyBookingCancellationReportData>

  @Query(
    """
    SELECT
      CAST(turnaround.Id AS VARCHAR) AS turnaroundId,
      CAST(bedspace.id AS VARCHAR) AS bedspaceId,
      CAST(booking.id AS VARCHAR) AS bookingId,
      turnaround.created_at AS CreatedAt,
      working_day_count AS workingDayCount
    From cas3_turnarounds turnaround
    INNER JOIN bookings booking ON booking.id = turnaround.booking_id
    INNER JOIN cas3_bedspaces bedspace ON bedspace.id = booking.bed_id
    INNER JOIN cas3_premises premises ON booking.premises_id = premises.id
    INNER JOIN probation_delivery_units pdu ON premises.probation_delivery_unit_id = pdu.id
    INNER JOIN probation_regions probation_region ON probation_region.id = pdu.probation_region_id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
      AND booking.arrival_date <= :endDate AND booking.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun findAllBookingTurnaroundByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedspaceOccupancyBookingTurnaroundReportData>

  @Query(
    """
    SELECT
        CAST(b.id AS VARCHAR) AS bedspaceId,
        vb.start_date AS startDate,
        vb.end_date AS endDate,
        null AS cancellationId
    From cas3_void_bedspaces vb
    LEFT JOIN cas3_bedspaces b ON vb.bedspace_id = b.id
    LEFT JOIN cas3_premises p ON b.premises_id = p.id
    INNER JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
    WHERE (CAST(:probationRegionId AS UUID) IS NULL OR pdu.probation_region_id = :probationRegionId)
      AND vb.start_date <= :endDate AND vb.end_date >= :startDate
      AND vb.cancellation_date is NULL
    ORDER BY vb.id     
    """,
    nativeQuery = true,
  )
  fun findAllVoidBedspaceByOverlappingDate(probationRegionId: UUID?, startDate: LocalDate, endDate: LocalDate): List<BedspaceOccupancyVoidBedspaceReportData>
}
