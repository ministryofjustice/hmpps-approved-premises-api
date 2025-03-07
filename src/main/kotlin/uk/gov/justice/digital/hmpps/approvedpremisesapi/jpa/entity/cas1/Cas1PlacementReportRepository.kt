package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1PlacementReportRepository(
  private val reportJdbcTemplate: ReportJdbcTemplate,
  private val cas1PlacementMatchingOutcomesV2ReportRepository: Cas1PlacementMatchingOutcomesV2ReportRepository,
) {

  companion object {
    private const val SELECT_DATASET_QUERY = """
      SELECT 
            csb.id AS placement_id,
            p.name AS premises_name,
            pr.name AS premises_region,
            to_char(csb.expected_arrival_date, 'YYYY-MM-DD') AS expected_arrival_date,
            COALESCE(csb.expected_departure_date - csb.expected_arrival_date, 0) AS expected_duration_nights,
            to_char(csb.expected_departure_date,'YYYY-MM-DD') AS expected_departure_date,
            to_char(csb.actual_arrival_date, 'YYYY-MM-DD') AS actual_arrival_date,
            to_char(csb.actual_arrival_time, 'HH24:MI:SS') AS actual_arrival_time,
            COALESCE(csb.actual_departure_date - csb.actual_arrival_date, 0) AS actual_duration_nights,
            to_char(csb.actual_departure_date, 'YYYY-MM-DD') AS actual_departure_date,
            to_char(csb.actual_departure_time, 'HH24:MI:SS') AS actual_departure_time,
            dr.name AS departure_reason,
            moc.name AS departure_move_on_category,
            to_char(CAST(csb.non_arrival_confirmed_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS non_arrival_recorded_date_time,
            nar.name AS non_arrival_reason,
            to_char(csb.cancellation_occurred_at,'YYYY-MM-DD') AS placement_withdrawn_date,
            to_char(CAST(csb.cancellation_recorded_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS placement_withdrawal_recorded_date_time,
            cr.name AS placement_withdrawn_reason,
            criteria_grouped.criteria AS criteria,
            matching.*
        FROM cas1_space_bookings csb
        INNER JOIN placement_matching_outcomes matching ON matching.placement_request_id = csb.placement_request_id
        LEFT JOIN premises p ON p.id = csb.premises_id
        LEFT JOIN probation_regions pr ON pr.id = p.probation_region_id
        LEFT JOIN departure_reasons dr ON dr.id = csb.departure_reason_id
        LEFT JOIN move_on_categories moc ON moc.id = csb.departure_move_on_category_id
        LEFT JOIN non_arrival_reasons nar ON nar.id = csb.non_arrival_reason_id
        LEFT JOIN cancellation_reasons cr ON cr.id = csb.cancellation_reason_id
        LEFT JOIN (
            SELECT 
                criteria.space_booking_id,
                STRING_AGG(c.property_name, ', ' ORDER BY c.property_name) AS criteria
            FROM cas1_space_bookings_criteria criteria
            LEFT JOIN "characteristics" c ON c.id = criteria.characteristic_id
            GROUP BY criteria.space_booking_id
        ) criteria_grouped ON criteria_grouped.space_booking_id = csb.id
        WHERE 
            (
                csb.canonical_arrival_date >= :startDateTimeInclusive 
                AND csb.canonical_arrival_date <= :endDateTimeInclusive
            ) OR (
                csb.canonical_departure_date >= :startDateTimeInclusive 
                AND csb.canonical_departure_date <= :endDateTimeInclusive
            ) OR (
                csb.non_arrival_confirmed_at >= :startDateTimeInclusive 
                AND csb.non_arrival_confirmed_at <= :endDateTimeInclusive
            ) OR (
                csb.cancellation_occurred_at >= :startDateTimeInclusive 
                AND csb.cancellation_occurred_at <= :endDateTimeInclusive
            )
        ORDER BY csb.expected_arrival_date ASC

      """
  }

  private fun buildPlacementReportQuery(): String {
    val placementMatchingOutcomesQuery = cas1PlacementMatchingOutcomesV2ReportRepository.buildQueryForPlacementReport()

    return """
            WITH placement_matching_outcomes AS ($placementMatchingOutcomesQuery)
            $SELECT_DATASET_QUERY
    """.trimIndent()
  }

  fun generatePlacementReport(
    startDateTimeInclusive: LocalDateTime,
    endDateTimeInclusive: LocalDateTime,
    jdbcResultSetConsumer: JdbcResultSetConsumer,
  ) {
    val query = buildPlacementReportQuery()

    reportJdbcTemplate.query(
      query,
      mapOf(
        "startDateTimeInclusive" to startDateTimeInclusive,
        "endDateTimeInclusive" to endDateTimeInclusive,
      ),
      jdbcResultSetConsumer,
    )
  }
}
