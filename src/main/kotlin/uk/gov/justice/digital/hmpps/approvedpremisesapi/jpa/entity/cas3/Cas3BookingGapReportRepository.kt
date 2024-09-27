package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

val GAP_RANGES_QUERY = """
with bed_bookings as (select
    probation_regions.name as probation_region,
    probation_delivery_units.name as pdu_name,
    premises.name as premises_name,
    rooms.name as room_name,
    range_agg(daterange(arrival_date, departure_date,'[]')) as booked_days,
    lower(range_agg(daterange(arrival_date, departure_date))) as date_min
from bookings
   left join premises on premises.id = bookings.premises_id
   left join probation_regions on probation_regions.id = premises.probation_region_id
   left join temporary_accommodation_premises on temporary_accommodation_premises.premises_id = bookings.premises_id
   left join probation_delivery_units on probation_delivery_units.id = temporary_accommodation_premises.probation_delivery_unit_id
   left join beds on beds.id = bookings.bed_id
   left join rooms on rooms.id = beds.room_id
   left join confirmations on confirmations.booking_id = bookings.id
   left join cancellations on  cancellations.booking_id = bookings.id
where bookings.service = 'temporary-accommodation' and cancellations.id is null
group by probation_region, pdu_name, premises_name, room_name
order by probation_region, pdu_name, premises_name, room_name),
booking_gaps as (select
    probation_region,
    pdu_name,
    premises_name,
    room_name,
    unnest(multirange(daterange(date_min, current_date)) - booked_days) as gap
from bed_bookings) 
select
    probation_region,
    pdu_name,
    premises_name,
    room_name,
    gap::text,
    upper(gap) - lower(gap) as gap_days
from booking_gaps
""".trimIndent()

@Repository
class Cas3BookingGapReportRepository(
  val jdbcTemplate: NamedParameterJdbcTemplate,
) {

  fun generateBookingGapRangesReport(): MutableList<MutableMap<String, Any>> {
    return jdbcTemplate.query(GAP_RANGES_QUERY.trimIndent(), ColumnMapRowMapper())
  }
}
