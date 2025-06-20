package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.repository.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.repository.ReportJdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class Cas1OverduePlacementsReportRepository(
  private val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {
    const val QUERY = """
      WITH active_overdue__bookings AS (
          SELECT
              sb.premises_id,
              sb.crn,
              sb.expected_arrival_date,
              sb.expected_departure_date,
              CASE
                  WHEN sb.actual_arrival_date IS NULL AND sb.expected_arrival_date < :current_date
                      THEN (:current_date - sb.expected_arrival_date)::INTEGER
                  END AS arrival_overdue_by_days,
              CASE
                  WHEN sb.actual_arrival_date IS NOT NULL AND sb.actual_departure_date IS NULL AND sb.expected_departure_date < :current_date
                      THEN (:current_date - sb.expected_departure_date)::INTEGER
                  END AS departure_overdue_by_days
          FROM
              cas1_space_bookings sb
          WHERE
              sb.cancellation_occurred_at IS NULL
            AND sb.non_arrival_confirmed_at IS NULL
            AND (
              (
                sb.actual_arrival_date IS NULL
                   AND sb.expected_arrival_date < :current_date
              )
            OR
              (
                sb.actual_arrival_date IS NOT NULL
                    AND sb.actual_departure_date IS NULL
                    AND sb.expected_departure_date < :current_date
                )
              )
            AND (
              (sb.expected_arrival_date >= :startDate AND sb.expected_arrival_date <= :endDate)
            OR
              (sb.expected_departure_date >= :startDate AND sb.expected_departure_date <= :endDate)
              )
            AND sb.expected_departure_date >= :expected_departure_threshold_date::DATE
      )
      SELECT
          p.name AS premises_name,
          aa.name AS premises_area,
          ap.gender AS premises_gender,
          aob.crn AS crn,
          TO_CHAR(aob.expected_arrival_date, 'YYYY-MM-DD') AS arrival_expected_date,
          aob.arrival_overdue_by_days,
          TO_CHAR(aob.expected_departure_date, 'YYYY-MM-DD') AS departure_expected_date,
          aob.departure_overdue_by_days
      FROM
          active_overdue__bookings aob
              INNER JOIN approved_premises ap ON aob.premises_id = ap.premises_id
              INNER JOIN premises p on ap.premises_id = p.id
              INNER JOIN probation_regions pr ON p.probation_region_id = pr.id
              INNER JOIN ap_areas aa on pr.ap_area_id = aa.id
        """
  }

  fun generateCas1OverduePlacementsReport(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    jdbcResultSetConsumer: JdbcResultSetConsumer,
    currentDate: LocalDate,
  ) = reportJdbcTemplate.query(
    QUERY,
    mapOf<String, Any>(
      "startDate" to startDate,
      "endDate" to endDate,
      "expected_departure_threshold_date" to Cas1SpaceBookingRepository.UPCOMING_EXPECTED_DEPARTURE_THRESHOLD,
      "current_date" to currentDate,
    ),
    jdbcResultSetConsumer,
  )
}
