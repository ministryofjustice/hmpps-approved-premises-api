package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import java.time.LocalDate
import java.util.UUID

interface BedUsageRepository : JpaRepository<BedEntity, UUID> {

  @Query(
    """
    SELECT b.*
    FROM beds b 
    INNER JOIN rooms r ON b.room_id = r.id
    INNER JOIN premises p ON r.premises_id = p.id
    WHERE p.service = 'temporary-accommodation'
      AND (CAST(:probationRegionId AS UUID) IS NULL OR p.probation_region_id = :probationRegionId)
    """,
    nativeQuery = true,
  )
  fun findAllBedspaces(
    probationRegionId: UUID?,
  ): List<BedEntity>

  @Query(
    value = """
        SELECT
            b.id                    AS bedId,
            boo.id                  AS bookingId,
            pr.id                   AS probationRegionId,
            pr.name                 AS probationRegionName,
            pdu.name                AS pdu,
            lau.name                AS localAuthorityArea,
            p.name                  AS propertyRef,
            p.address_line1         AS addressLine1,
            p.town                  AS town,
            p.postcode              AS postCode,
            r.name                  AS bedspaceRef,
            boo.crn                 AS crn,
            'Booking'               AS type,
            boo.departure_date      AS bookingDepartureDate,
            boo.arrival_date        AS startDate,
            boo.departure_date      AS endDate,
            t.id                    AS turnaroundId,
            t.working_day_count     AS turnaroundWorkingDayCount,
            COUNT(DISTINCT c.id)    AS cancellationCount,
            COUNT(DISTINCT d.id)    AS departureCount,
            COUNT(DISTINCT a.id)    AS arrivalCount,
            COUNT(DISTINCT na.id)   AS nonArrivalCount,
            COUNT(DISTINCT conf.id) AS confirmationCount,
            NULL                    AS voidCategory,
            NULL                    AS voidNotes,
            NULL                    AS costCentre,
            p.id                    AS uniquePropertyRef,
            r.id                    AS uniqueBedspaceRef
        FROM temporary_accommodation_premises tap
                 JOIN premises p ON p.id = tap.premises_id
                 JOIN rooms r ON r.premises_id = p.id
                 JOIN beds b ON b.room_id = r.id
                 JOIN bookings boo ON boo.bed_id = b.id
                 LEFT JOIN cas3_turnarounds t ON t.booking_id = boo.id
                 LEFT JOIN cancellations c ON c.booking_id = boo.id
                 LEFT JOIN departures d ON d.booking_id = boo.id
                 LEFT JOIN arrivals a ON a.booking_id = boo.id
                 LEFT JOIN non_arrivals na ON na.booking_id = boo.id
                 LEFT JOIN cas3_confirmations conf ON conf.booking_id = boo.id
                 LEFT JOIN probation_regions pr ON pr.id = p.probation_region_id
                 LEFT JOIN probation_delivery_units pdu ON pdu.id = tap.probation_delivery_unit_id
                 LEFT JOIN local_authority_areas lau ON lau.id = p.local_authority_area_id
        WHERE p.service = 'temporary-accommodation'
          AND p.probation_region_id = :probationRegionId
          AND boo.arrival_date <= :endDate
          AND boo.departure_date >= :startDate
        GROUP BY
            b.id, boo.id,
            pr.id, pr.name,
            pdu.name,
            lau.name,
            p.name, p.address_line1, p.town, p.postcode,
            r.name,
            boo.crn,
            boo.departure_date,
            boo.arrival_date,
            t.id, t.working_day_count,
            p.id,
            r.id
            
        UNION
        
        SELECT 
            b.id                AS bedId,
            boo.id              AS bookingId,
            pr.id               AS probationRegionId,
            pr.name             AS probationRegionName,
            pdu.name            AS pdu,
            lau.name            AS localAuthorityArea,
            p.name              AS propertyRef,
            p.address_line1     AS addressLine1,
            p.town              AS town,
            p.postcode          AS postCode,
            r.name              AS bedspaceRef,
            NULL                AS crn,
            'Turnaround'        AS type,
            boo.departure_date  AS bookingDepartureDate,
            NULL                AS startDate,
            NULL                AS endDate,
            t.id                AS turnaroundId,
            t.working_day_count AS turnaroundWorkingDayCount,
            0                   AS cancellationCount,
            0                   AS departureCount,
            0                   AS arrivalCount,
            0                   AS nonArrivalCount,
            0                   AS confirmationCount,
            NULL                AS voidCategory,
            NULL                AS voidNotes,
            NULL                AS costCentre,
            p.id                AS uniquePropertyRef,
            r.id                AS uniqueBedspaceRef
        FROM temporary_accommodation_premises tap
                 JOIN premises p on p.id = tap.premises_id
                 JOIN rooms r on r.premises_id = p.id
                 JOIN beds b on b.room_id = r.id
                 JOIN bookings boo on boo.bed_id = b.id
                 JOIN cas3_turnarounds t on t.booking_id = boo.id
                 LEFT JOIN probation_regions pr on pr.id = p.probation_region_id
                 LEFT JOIN probation_delivery_units pdu ON pdu.id = tap.probation_delivery_unit_id
                 LEFT JOIN local_authority_areas lau on lau.id = p.local_authority_area_id
        WHERE p.service = 'temporary-accommodation'
          AND p.probation_region_id = :probationRegionId
          AND boo.arrival_date <= :endDate
          AND boo.departure_date >= :startDate
          
        UNION
        
        SELECT 
            b.id            AS bedId,
            NULL            AS bookingId,
            pr.id           AS probationRegionId,
            pr.name         AS probationRegionName,
            pdu.name        AS pdu,
            lau.name        AS localAuthorityArea,
            p.name          AS propertyRef,
            p.address_line1 AS addressLine1,
            p.town          AS town,
            p.postcode      AS postCode,
            r.name          AS bedspaceRef,
            NULL            AS crn,
            'Void'          AS type,
            NULL            AS bookingDepartureDate,
            lb.start_date   AS startDate,
            lb.end_date     AS endDate,
            NULL            AS turnaroundId,
            NULL            AS turnaroundWorkingDayCount,
            0               AS cancellationCount,
            0               AS departureCount,
            0               AS arrivalCount,
            0               AS nonArrivalCount,
            0               AS confirmationCount,
            lbr.name        AS voidCategory,
            lb.notes        AS voidNotes,
            lb.cost_centre  AS costCentre,
            p.id            AS uniquePropertyRef,
            r.id            AS uniqueBedspaceRef
        FROM temporary_accommodation_premises tap
                 JOIN premises p on p.id = tap.premises_id
                 JOIN rooms r on r.premises_id = p.id
                 JOIN beds b on b.room_id = r.id
                 JOIN cas3_void_bedspaces lb on lb.bed_id = b.id
                 LEFT JOIN cas3_void_bedspace_reasons lbr on lbr.id = lb.cas3_void_bedspace_reason_id
                 LEFT JOIN probation_regions pr on pr.id = p.probation_region_id
                 LEFT JOIN probation_delivery_units pdu ON pdu.id = tap.probation_delivery_unit_id
                 LEFT JOIN local_authority_areas lau on lau.id = p.local_authority_area_id
        WHERE p.service = 'temporary-accommodation'
          AND p.probation_region_id = :probationRegionId
          AND lb.start_date <= :endDate
          AND lb.end_date >= :startDate
          AND lb.cancellation_date IS NULL
          
        ORDER BY bedId, bookingId
        """,
    nativeQuery = true,
  )
  fun findBedUsageReportData(
    probationRegionId: UUID?,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<BedUsageReportDataRow>
}

interface BedUsageReportDataRow {
  val bedId: UUID
  val bookingId: UUID?
  val probationRegionId: String?
  val probationRegionName: String?
  val pdu: String?
  val localAuthorityArea: String?
  val propertyRef: String
  val addressLine1: String
  val town: String?
  val postCode: String
  val bedspaceRef: String
  val crn: String?
  val type: String
  val bookingDepartureDate: LocalDate
  val startDate: LocalDate
  val endDate: LocalDate
  val turnaroundId: UUID?
  val turnaroundWorkingDayCount: Int?
  val cancellationCount: Int
  val departureCount: Int
  val arrivalCount: Int
  val nonArrivalCount: Int
  val confirmationCount: Int
  val voidCategory: String?
  val voidNotes: String?
  val costCentre: String?
  val uniquePropertyRef: String
  val uniqueBedspaceRef: String
}

data class BedUsageReportDataDTO(
  val bedId: UUID,
  val bookingId: UUID?,
  val probationRegionId: String?,
  val probationRegionName: String?,
  val pdu: String?,
  val localAuthorityArea: String?,
  val propertyRef: String,
  val addressLine1: String,
  val town: String?,
  val postCode: String,
  val bedspaceRef: String,
  val crn: String?,
  val type: String,
  val bookingDepartureDate: LocalDate,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val turnaroundId: UUID?,
  val turnaroundWorkingDayCount: Int?,
  val cancellationCount: Int,
  val departureCount: Int,
  val arrivalCount: Int,
  val nonArrivalCount: Int,
  val confirmationCount: Int,
  val voidCategory: String?,
  val voidNotes: String?,
  val costCentre: String?,
  val uniquePropertyRef: String,
  val uniqueBedspaceRef: String,
)
