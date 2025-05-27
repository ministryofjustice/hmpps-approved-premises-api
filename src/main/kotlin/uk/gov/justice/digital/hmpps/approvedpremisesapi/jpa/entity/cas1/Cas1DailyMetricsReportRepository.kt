package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDate

@Repository
class Cas1DailyMetricsReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {

    const val QUERY = """
       WITH all_dates AS (
           SELECT generate_series(
               :start_date::DATE,
               :end_date::DATE,
               '1 day'::INTERVAL
           )::DATE AS report_date
         ),
       applications_by_day AS (
           SELECT
               DATE(application.created_at) AS report_date,
               COUNT(*) AS applications_started,
               COUNT(DISTINCT CAST(application.created_by_user_id AS TEXT)) AS unique_users_starting_applications
           FROM approved_premises_applications apa
               INNER JOIN applications application ON application.id = apa.id
           WHERE application.created_at BETWEEN :start_date AND :end_date
           GROUP BY DATE(application.created_at)
         ),
       domain_events_by_day AS (
           SELECT
               DATE(domain_event.occurred_at) AS report_date,
               COUNT(*) FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED') AS applications_submitted,
               COUNT(DISTINCT CAST(domain_event.data->'eventDetails'->'submittedBy'->'staffMember'->>'staffCode' AS TEXT))
               FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED') AS unique_users_submitting_applications,
               COUNT(*) FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED') AS assessments_completed,
               COUNT(DISTINCT CAST(domain_event.data->'eventDetails'->'assessedBy'->'staffMember'->>'staffCode' AS TEXT))
               FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED') AS unique_users_completing_assessments,
               COUNT(*) FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_BOOKING_MADE') AS bookings_made,
               COUNT(DISTINCT CAST(domain_event.data->'eventDetails'->>'bookedBy' AS TEXT))
               FILTER (WHERE domain_event.type = 'APPROVED_PREMISES_BOOKING_MADE') AS unique_users_making_bookings
           FROM domain_events domain_event
           WHERE domain_event.occurred_at BETWEEN :start_date AND :end_date
           AND domain_event.service='CAS1'
           GROUP BY DATE(domain_event.occurred_at)
        )
       SELECT
           TO_CHAR(dates.report_date, 'YYYY-MM-DD') AS report_date,
           COALESCE(applications_started, 0) AS applications_started,
           COALESCE(unique_users_starting_applications, 0) AS unique_users_starting_applications,
           COALESCE(applications_submitted, 0) AS applications_submitted,
           COALESCE(unique_users_submitting_applications, 0) AS unique_users_submitting_applications,
           COALESCE(assessments_completed, 0) AS assessments_completed,
           COALESCE(unique_users_completing_assessments, 0) AS unique_users_completing_assessments,
           COALESCE(bookings_made, 0) AS bookings_made,
           COALESCE(unique_users_making_bookings, 0) AS unique_users_making_bookings
       FROM all_dates dates
           LEFT JOIN applications_by_day ON dates.report_date = applications_by_day.report_date
           LEFT JOIN domain_events_by_day ON dates.report_date = domain_events_by_day.report_date
       ORDER BY dates.report_date
  """
  }

  fun generateCas1DailyMetricsReport(
    startDate: LocalDate,
    endDate: LocalDate,
    jdbcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    QUERY,
    mapOf<String, Any>(
      "start_date" to startDate,
      "end_date" to endDate,
    ),
    jdbcResultSetConsumer,
  )
}
