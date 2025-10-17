package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.ReportJdbcTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BedspaceVoid
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingRecord
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SqlUtil.getUUID
import java.time.LocalDate

val GAP_RANGES_QUERY = """
with bedspaces as (select
                        beds.id,
                        premises.name as premises_name,
                        rooms.name as room_name,
                        probation_regions.name as probation_region,
                        probation_delivery_units.name as pdu_name,
                        beds.end_date
                    from beds
                    inner join rooms on beds.room_id = rooms.id
                    inner join premises on rooms.premises_id = premises.id
                    inner join probation_regions on probation_regions.id = premises.probation_region_id
                    inner join temporary_accommodation_premises on temporary_accommodation_premises.premises_id = premises.id
                    inner join probation_delivery_units on probation_delivery_units.id = temporary_accommodation_premises.probation_delivery_unit_id
                    where (beds.end_date is null or beds.end_date >= :startDate) and beds.created_at <= :endDate
),
 all_bookings as (select
                      bed_id,
                      arrival_date,
                      departure_date,
                      (select working_day_count
                       from cas3_turnarounds
                       where cas3_turnarounds.id = (
                           select id
                           from cas3_turnarounds
                           where  cas3_turnarounds.booking_id = bookings.id
                           order by cas3_turnarounds.created_at desc
                           limit 1)) turnaround_days
                  from bookings
                  left join cancellations on  cancellations.booking_id = bookings.id
                  where bookings.service = 'temporary-accommodation' and cancellations.id is null and arrival_date <= :endDate and departure_date >= :startDate and arrival_date != departure_date),
 bedspace_bookings as (select 
                        bedspaces.id as bed_id,
                        premises_name,
                        room_name,
                        probation_region,
                        pdu_name,
                        arrival_date,
                        departure_date,
                        turnaround_days
                 from bedspaces
                 inner join all_bookings on bedspaces.id = all_bookings.bed_id
                 order by premises_name,room_name,arrival_date),
 bedspace_voids as(select
                     bed_id,
                     premises_name,
                     room_name,
                     probation_region,
                     pdu_name,
                     voids.start_date,
                     voids.end_date
                  from cas3_void_bedspaces voids
                  inner join bedspaces on voids.bed_id = bedspaces.id
                  where voids.start_date <= :endDate and voids.end_date >= :startDate
 ),
 bedspace_unavailable_dates as(
    select
        bed_id,
        premises_name,
        room_name,
        probation_region,
        pdu_name,
        arrival_date as start_date,
        departure_date as end_date,
        turnaround_days
    from bedspace_bookings
    where arrival_date is not null and departure_date is not null
    union all
    select bedspace_voids.*, 0 as turnaround_days
    from bedspace_voids),
 bedspace_unavailable_ranges as (select
                                   bed_id,
                                   probation_region,
                                   pdu_name,
                                   premises_name,
                                   room_name,
                                   range_agg(daterange(start_date, end_date,'[]')) as unavailable_days_range,
                                   range_agg(daterange(start_date, end_date-1,'[]')) as unavailable_days,
                                   lower(range_agg(daterange(start_date, end_date))) as date_min,
                                   upper(range_agg(daterange(start_date, end_date))) as date_max
                               from bedspace_unavailable_dates
                               where start_date != end_date
                               group by bed_id,probation_region, pdu_name, premises_name, room_name
                               order by bed_id,probation_region, pdu_name, premises_name, room_name),
bedspace_gaps as (select
                   probation_region,
                   pdu_name,
                   premises_name,
                   room_name,
                   unnest(multirange(daterange(least(date_min,:startDate), greatest(date_max,:endDate))) - unavailable_days_range) as gap_range,
                   unnest(multirange(daterange(least(date_min,:startDate), greatest(date_max,:endDate))) - unavailable_days) as gap
               from bedspace_unavailable_ranges
               union all
               -- bedspace do not have bookings or voids during the report period
               select
                   probation_region,
                   pdu_name,
                   premises_name,
                   room_name,
                   daterange(:startDate,:endDate) as gap_range,
                   daterange(:startDate,:endDate) as gap
               from bedspaces
               where bedspaces.id not in (select bed_id from bedspace_bookings)
                 and bedspaces.id not in (select bed_id from bedspace_voids)),
bedspace_gaps_and_days as (select distinct
                            bedspace_gaps.probation_region,
                            bedspace_gaps.pdu_name,
                            bedspace_gaps.premises_name,
                            bedspace_gaps.room_name as bed_name,
                            coalesce(bedspace_gaps.gap_range,bedspace_gaps.gap)::text as gap,
                            case
                                when upper(gap) = :endDate then upper(gap) + 1
                                else upper(gap)
                                end -
                            case
                                when lower(gap) = :startDate then lower(gap)
                                else lower(gap) + 1
                                end as gap_days,
                                (select turnaround_days
                                    from bedspace_bookings
                                    where bedspace_bookings.premises_name = bedspace_gaps.premises_name and bedspace_bookings.room_name = bedspace_gaps.room_name
                                    limit 1
                                    ) as turnaround_days
                        from bedspace_gaps)

select *
from bedspace_gaps_and_days
where gap_days > 0
order by probation_region,pdu_name,premises_name,bed_name,gap
""".trimIndent()

val GAP_RANGES_QUERY_V2 = """
with bedspaces as (select
                       bedspace.id,
                       premises.name as premises_name,
                       bedspace.reference as room_name,
                       probation_regions.name as probation_region,
                       pdu.name as pdu_name,
                       bedspace.end_date
                   from cas3_bedspaces bedspace
                            inner join cas3_premises premises on bedspace.premises_id = premises.id
                            inner join probation_delivery_units pdu on pdu.id = premises.probation_delivery_unit_id
                            inner join probation_regions on probation_regions.id = pdu.probation_region_id
                   where (bedspace.end_date is null or bedspace.end_date >= :startDate) and bedspace.created_at <= :endDate
),
 all_bookings as (select
                      bed_id,
                      arrival_date,
                      departure_date,
                      (select working_day_count
                       from cas3_turnarounds
                       where cas3_turnarounds.id = (
                           select id
                           from cas3_turnarounds
                           where  cas3_turnarounds.booking_id = bookings.id
                           order by cas3_turnarounds.created_at desc
                           limit 1)) turnaround_days
                  from bookings
                  left join cancellations on  cancellations.booking_id = bookings.id
                  where bookings.service = 'temporary-accommodation' and cancellations.id is null and arrival_date <= :endDate and departure_date >= :startDate and arrival_date != departure_date),
 bedspace_bookings as (select 
                        bedspaces.id as bed_id,
                        premises_name,
                        room_name,
                        probation_region,
                        pdu_name,
                        arrival_date,
                        departure_date,
                        turnaround_days
                 from bedspaces
                 inner join all_bookings on bedspaces.id = all_bookings.bed_id
                 order by premises_name,room_name,arrival_date),
 bedspace_voids as(select
                     bedspace_id as bed_id,
                     premises_name,
                     room_name,
                     probation_region,
                     pdu_name,
                     voids.start_date,
                     voids.end_date
                  from cas3_void_bedspaces voids
                  inner join bedspaces on voids.bedspace_id = bedspaces.id
                  where voids.start_date <= :endDate and voids.end_date >= :startDate
 ),
 bedspace_unavailable_dates as(
    select
        bed_id,
        premises_name,
        room_name,
        probation_region,
        pdu_name,
        arrival_date as start_date,
        departure_date as end_date,
        turnaround_days
    from bedspace_bookings
    where arrival_date is not null and departure_date is not null
    union all
    select bedspace_voids.*, 0 as turnaround_days
    from bedspace_voids),
 bedspace_unavailable_ranges as (select
                                   bed_id,
                                   probation_region,
                                   pdu_name,
                                   premises_name,
                                   room_name,
                                   range_agg(daterange(start_date, end_date,'[]')) as unavailable_days_range,
                                   range_agg(daterange(start_date, end_date-1,'[]')) as unavailable_days,
                                   lower(range_agg(daterange(start_date, end_date))) as date_min,
                                   upper(range_agg(daterange(start_date, end_date))) as date_max
                               from bedspace_unavailable_dates
                               where start_date != end_date
                               group by bed_id,probation_region, pdu_name, premises_name, room_name
                               order by bed_id,probation_region, pdu_name, premises_name, room_name),
bedspace_gaps as (select
                   probation_region,
                   pdu_name,
                   premises_name,
                   room_name,
                   unnest(multirange(daterange(least(date_min,:startDate), greatest(date_max,:endDate))) - unavailable_days_range) as gap_range,
                   unnest(multirange(daterange(least(date_min,:startDate), greatest(date_max,:endDate))) - unavailable_days) as gap
               from bedspace_unavailable_ranges
               union all
               -- bedspace do not have bookings or voids during the report period
               select
                   probation_region,
                   pdu_name,
                   premises_name,
                   room_name,
                   daterange(:startDate,:endDate) as gap_range,
                   daterange(:startDate,:endDate) as gap
               from bedspaces
               where bedspaces.id not in (select bed_id from bedspace_bookings)
                 and bedspaces.id not in (select bed_id from bedspace_voids)),
bedspace_gaps_and_days as (select distinct
                            bedspace_gaps.probation_region,
                            bedspace_gaps.pdu_name,
                            bedspace_gaps.premises_name,
                            bedspace_gaps.room_name as bed_name,
                            coalesce(bedspace_gaps.gap_range,bedspace_gaps.gap)::text as gap,
                            case
                                when upper(gap) = :endDate then upper(gap) + 1
                                else upper(gap)
                                end -
                            case
                                when lower(gap) = :startDate then lower(gap)
                                else lower(gap) + 1
                                end as gap_days,
                                (select turnaround_days
                                    from bedspace_bookings
                                    where bedspace_bookings.premises_name = bedspace_gaps.premises_name and bedspace_bookings.room_name = bedspace_gaps.room_name
                                    limit 1
                                    ) as turnaround_days
                        from bedspace_gaps)

select *
from bedspace_gaps_and_days
where gap_days > 0
order by probation_region,pdu_name,premises_name,bed_name,gap
""".trimIndent()

@Repository
class Cas3BookingGapReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
  val jdbcTemplate: NamedParameterJdbcTemplate,
) {

  fun generateBookingGapReport(
    startDate: LocalDate,
    endDate: LocalDate,
    jdbcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    GAP_RANGES_QUERY.trimIndent(),
    mapOf<String, Any>(
      "startDate" to startDate,
      "endDate" to endDate,
    ),
    jdbcResultSetConsumer,
  )

  fun generateBookingGapReportV2(
    startDate: LocalDate,
    endDate: LocalDate,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    GAP_RANGES_QUERY_V2.trimIndent(),
    mapOf<String, Any>(
      "startDate" to startDate,
      "endDate" to endDate,
    ),
    jbdcResultSetConsumer,
  )

  fun getBedspaces(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo> {
    val query = """
        SELECT beds.id,
               premises.name AS premises_name,
               rooms.name AS room_name,
               probation_regions.name AS probation_region,
               probation_delivery_units.name AS pdu_name,
               beds.start_date,
               beds.end_date
        FROM beds
        INNER JOIN rooms ON beds.room_id = rooms.id
        INNER JOIN premises ON rooms.premises_id = premises.id
        INNER JOIN probation_regions ON probation_regions.id = premises.probation_region_id
        INNER JOIN temporary_accommodation_premises ON temporary_accommodation_premises.premises_id = premises.id
        INNER JOIN probation_delivery_units ON probation_delivery_units.id = temporary_accommodation_premises.probation_delivery_unit_id
        WHERE (beds.end_date IS NULL OR beds.end_date >= :startDate) AND beds.start_date <= :endDate
    """.trimIndent()

    return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
      BedspaceInfo(
        id = rs.getUUID("id"),
        premisesName = rs.getString("premises_name"),
        roomName = rs.getString("room_name"),
        probationRegion = rs.getString("probation_region"),
        pduName = rs.getString("pdu_name"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date")?.toLocalDate(),
      )
    }
  }

  fun getBedspacesV2(startDate: LocalDate, endDate: LocalDate): List<BedspaceInfo> {
    val query = """
        SELECT bedspace.id,
               premises.name AS premises_name,
               bedspace.reference AS room_name,
               probation_regions.name AS probation_region,
               pdu.name AS pdu_name,
               bedspace.start_date,
               bedspace.end_date
        FROM cas3_bedspaces bedspace
        INNER JOIN cas3_premises premises ON bedspace.premises_id = premises.id
        INNER JOIN probation_delivery_units pdu ON pdu.id = premises.probation_delivery_unit_id
        INNER JOIN probation_regions ON probation_regions.id = pdu.probation_region_id
        WHERE (bedspace.end_date IS NULL OR bedspace.end_date >= :startDate) AND bedspace.start_date <= :endDate
    """.trimIndent()

    return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
      BedspaceInfo(
        id = rs.getUUID("id"),
        premisesName = rs.getString("premises_name"),
        roomName = rs.getString("room_name"),
        probationRegion = rs.getString("probation_region"),
        pduName = rs.getString("pdu_name"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date")?.toLocalDate(),
      )
    }
  }

  fun getBookings(startDate: LocalDate, endDate: LocalDate): List<BookingRecord> {
    val query = """
        SELECT bed_id,
               arrival_date,
               departure_date,
               (SELECT working_day_count
                FROM cas3_turnarounds
                WHERE cas3_turnarounds.id = (
                    SELECT id
                    FROM cas3_turnarounds
                    WHERE cas3_turnarounds.booking_id = bookings.id
                    ORDER BY cas3_turnarounds.created_at DESC
                    LIMIT 1)) turnaround_days
        FROM bookings
        LEFT JOIN cancellations ON cancellations.booking_id = bookings.id
        WHERE bookings.service = 'temporary-accommodation' 
          AND cancellations.id IS NULL 
          AND arrival_date <= :endDate 
          AND departure_date >= :startDate
    """.trimIndent()

    return jdbcTemplate.query(query, mapOf("startDate" to startDate, "endDate" to endDate)) { rs, _ ->
      BookingRecord(
        bedId = rs.getUUID("bed_id"),
        arrivalDate = rs.getDate("arrival_date").toLocalDate(),
        departureDate = rs.getDate("departure_date").toLocalDate(),
        turnaroundDays = rs.getObject("turnaround_days") as? Int,
      )
    }
  }

  fun getBedspaceVoids(startDate: LocalDate): List<BedspaceVoid> {
    val query = """
        SELECT voids.bed_id,
               voids.start_date,
               voids.end_date
        FROM cas3_void_bedspaces voids
        WHERE voids.cancellation_date IS NULL AND
            voids.end_date >= :startDate
    """.trimIndent()

    return jdbcTemplate.query(query, mapOf("startDate" to startDate)) { rs, _ ->
      BedspaceVoid(
        bedId = rs.getUUID("bed_id"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
      )
    }
  }

  fun getBedspaceVoidsV2(startDate: LocalDate): List<BedspaceVoid> {
    val query = """
        SELECT voids.bedspace_id AS bed_id,
               voids.start_date,
               voids.end_date
        FROM cas3_void_bedspaces voids
        WHERE voids.cancellation_date IS NULL AND
            voids.end_date >= :startDate
    """.trimIndent()

    return jdbcTemplate.query(query, mapOf("startDate" to startDate)) { rs, _ ->
      BedspaceVoid(
        bedId = rs.getUUID("bed_id"),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
      )
    }
  }
}
