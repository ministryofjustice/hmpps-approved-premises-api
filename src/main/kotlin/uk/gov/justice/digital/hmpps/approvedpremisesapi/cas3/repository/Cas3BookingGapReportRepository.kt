package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceVoid
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingRecord
import java.time.LocalDate
import java.util.UUID

@Repository
interface Cas3BookingGapReportRepository : JpaRepository<Cas3VoidBedspaceEntity, UUID> {

  @Query(
    """
        SELECT beds.id,
            premises.name AS premises_name,
            rooms.name AS room_name,
            probation_regions.name AS probation_region,
            probation_delivery_units.name AS pdu_name,
            beds.startDate,
            beds.endDate
        FROM BedEntity beds
        INNER JOIN RoomEntity rooms ON beds.room.id = rooms.id
        INNER JOIN PremisesEntity premises ON rooms.premises.id = premises.id
        INNER JOIN ProbationRegionEntity probation_regions ON probation_regions.id = premises.probationRegion.id
        INNER JOIN TemporaryAccommodationPremisesEntity temporary_accommodation_premises ON temporary_accommodation_premises.id = premises.id
        INNER JOIN ProbationDeliveryUnitEntity probation_delivery_units ON probation_delivery_units.id = temporary_accommodation_premises.probationDeliveryUnit.id
        WHERE (beds.endDate IS NULL OR beds.endDate >= :startDate) AND beds.startDate <= :endDate
    """,
  )
  fun getBedspaces(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo>

  @Query(
    """
       SELECT bedspace.id,
           premises.name AS premises_name,
           bedspace.reference AS room_name,
           probation_regions.name AS probation_region,
           pdu.name AS pdu_name,
           bedspace.startDate,
           bedspace.endDate
        FROM Cas3BedspacesEntity bedspace
        INNER JOIN Cas3PremisesEntity premises ON bedspace.premises.id = premises.id
        INNER JOIN ProbationDeliveryUnitEntity pdu ON pdu.id = premises.probationDeliveryUnit.id
        INNER JOIN ProbationRegionEntity probation_regions ON probation_regions.id = pdu.probationRegion.id
        WHERE (bedspace.endDate IS NULL OR bedspace.endDate >= :startDate) AND bedspace.startDate <= :endDate
    """,
  )
  fun getBedspacesV2(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo>

  @Query(
    """
      SELECT bookings.bed.id,
          bookings.arrivalDate,
          bookings.departureDate,
          (SELECT cas3_turnarounds.workingDayCount
          FROM Cas3TurnaroundEntity cas3_turnarounds
          WHERE cas3_turnarounds.id = (
              SELECT cas3_turnarounds.id
              FROM Cas3TurnaroundEntity cas3_turnarounds
              WHERE cas3_turnarounds.booking.id = bookings.id
              ORDER BY cas3_turnarounds.createdAt DESC
              LIMIT 1)) turnaround_days
      FROM BookingEntity bookings
      LEFT JOIN Cas3CancellationEntity cancellations ON cancellations.booking.id = bookings.id
      WHERE bookings.service = 'temporary-accommodation' 
        AND cancellations.id IS NULL 
        AND bookings.arrivalDate <= :endDate 
        AND bookings.departureDate >= :startDate
    """,
  )
  fun getBookings(startDate: LocalDate, endDate: LocalDate): List<BookingRecord>

  @Query(
    """
      SELECT 
          b.bed_id AS bedId,
          b.arrival_date AS arrivalDate,
          b.departure_date AS departureDate,
          t.working_day_count AS turnaroundDays
      FROM bookings b
      LEFT JOIN cancellations c ON c.booking_id = b.id
      LEFT JOIN (
          SELECT DISTINCT ON (booking_id) booking_id, working_day_count
          FROM cas3_turnarounds
          ORDER BY booking_id, created_at DESC
      ) t ON t.booking_id = b.id
      WHERE b.service = 'temporary-accommodation' 
        AND c.id IS NULL 
        AND b.arrival_date <= :endDate 
        AND b.departure_date >= :startDate
    """,
    nativeQuery = true,
  )
  fun getBookingsV2(startDate: LocalDate, endDate: LocalDate): List<BookingRecord>

  @Query(
    """
        SELECT void.bed.id,
            void.startDate,
            void.endDate
        FROM Cas3VoidBedspaceEntity void
        WHERE void.cancellationDate IS NULL AND void.endDate >= :startDate
    """,
  )
  fun getBedspaceVoids(startDate: LocalDate): List<BedspaceVoid>

  @Query(
    """
        SELECT void.bedspace.id AS bed_id,
            void.startDate,
            void.endDate
        FROM Cas3VoidBedspaceEntity void
        WHERE void.cancellationDate IS NULL AND void.endDate >= :startDate
      """,
  )
  fun getBedspaceVoidsV2(startDate: LocalDate): List<BedspaceVoid>
}
