package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class Cas1OccupancyReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  fun generate(
    fromInclusive: LocalDate,
    toInclusive: LocalDate,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    generateSql(fromInclusive, toInclusive),
    emptyMap<String, String>(),
    jbdcResultSetConsumer,
  )

  private fun generateSql(
    fromInclusive: LocalDate,
    toInclusive: LocalDate,
  ): String {
    val fromString = fromInclusive.format(DateTimeFormatter.ISO_DATE)
    val toString = toInclusive.format(DateTimeFormatter.ISO_DATE)

    return """
    select * from
      crosstab(
      ${'$'}${'$'}
    	with 
    	days as (
    		SELECT date_trunc('day', dd):: date as day
    	    FROM generate_series ('$fromString'::timestamp, '$toString'::timestamp, '1 day'::interval) dd
    	),
    	premises_and_days as (
    		SELECT 
    		days.day as day,
    		ap.premises_id as premises_id
    		from days
    		inner join approved_premises ap ON true
    		where ap.supports_space_bookings IS TRUE
    	),
    	ossb_latest_state as (
    		SELECT 
    			oosb.premises_id,
    			rev.start_date,
    			rev.end_date
    		FROM cas1_out_of_service_beds oosb
    		inner join lateral (
    			select *
    			from cas1_out_of_service_bed_revisions rev
    			where rev.out_of_service_bed_id = oosb.id
    			order by created_at desc
    			limit 1
    		) as rev on TRUE 
    		left outer join cas1_out_of_service_bed_cancellations cancellations on cancellations.out_of_service_bed_id = oosb.id
    		where cancellations.id IS NULL
    	),
    	premises_day_state as (
    		select 
    			premises_and_days.day as day,
    			p.id as premises_id,
    			concat(area.name,' - ',p.name) as premises_name,
    			(
    				select count(*) from beds 
    				inner join rooms on beds.room_id = rooms.id
    				where 
    				rooms.premises_id = premises_and_days.premises_id AND
    				(beds.end_date IS NULL or beds.end_date > premises_and_days.day)
    			) as beds_count,
    			(
    				select count(*) from ossb_latest_state oosb
    				where oosb.premises_id = premises_and_days.premises_id AND
    				(oosb.start_date <= premises_and_days.day AND oosb.end_date >= premises_and_days.day)
    			) as oosb_count,
    			(
    				select count(*) from cas1_space_bookings sb
    				where sb.premises_id = premises_and_days.premises_id AND
    				sb.cancellation_occurred_at IS NULL AND sb.non_arrival_confirmed_at IS NULL AND
    				(sb.canonical_arrival_date <= premises_and_days.day AND sb.canonical_departure_date > premises_and_days.day)
    			) as bookings_count
    		from premises_and_days
    		inner join premises p on p.id = premises_and_days.premises_id
        inner join probation_regions region ON region.id = p.probation_region_id
        inner join ap_areas area ON area.id = region.ap_area_id 
    	),
    	premises_day_state_summary as (
    		select 
    			premises_name,
    			day,
    			beds_count,
    			oosb_count,
    			bookings_count,
    			(beds_count - oosb_count - bookings_count:: int) as available_beds
    		from premises_day_state
    		order by premises_name, day asc
    	)
        select 
    		premises_name as row_name,
    		to_char(day, 'dd/mm/yyyy') as category,
    		concat(available_beds) :: text as value
    	from premises_day_state_summary order by premises_name, day
      ${'$'}${'$'})
      as capacity(
      row_name text,
      ${generateDateColumnListing(fromInclusive, toInclusive)})

    """.trimIndent()
  }

  private fun generateDateColumnListing(
    fromInclusive: LocalDate,
    toInclusive: LocalDate,
  ) = fromInclusive.datesUntil(toInclusive.plusDays(1))
    .toList()
    .joinToString(",") { "\"${it.dayOfMonth}/${it.monthValue}\" text" }
}
