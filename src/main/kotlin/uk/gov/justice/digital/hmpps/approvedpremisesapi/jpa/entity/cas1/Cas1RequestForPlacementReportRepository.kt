package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1RequestForPlacementReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  /**
   Because of the bifurcated placement request model, this query is a union from two sources:

   1. placement_applications_automatic

   Represents requests for placements implicit in the original application (where dates are defined)

   These are 'STANDARD' requests

   2. placement_applications

   Represents requests for placements made after the application has been assessed
   */
  fun buildQuery(
    placementRequestsRangeConstraints: String,
    placementApplicationsRangeConstraints: String,
  ): String {
    val s = """
  WITH raw_applications_report AS (
      ${Cas1ApplicationV2ReportRepository.COMPLETE_DATASET_QUERY}
  )
  
  SELECT 
    pr.id AS internal_placement_request_id,
    null AS internal_placement_application_date_id,
    CONCAT('placement_request:',paa.id) AS request_for_placement_id, 
    'STANDARD' AS request_for_placement_type,
    to_char(paa.expected_arrival_date, 'YYYY-MM-DD') AS requested_arrival_date,
    pr.duration AS requested_duration_days,
    to_char(CAST(a.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_submitted_date,
    null AS parole_decision_date,
    latest_assessment.decision AS request_for_placement_decision,
    to_char(CAST(latest_assessment.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_decision_made_date,
    CASE
      WHEN pr.id IS NULL THEN to_char(apa_withdrawn_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
      ELSE to_char(pr_withdrawn_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
     END AS request_for_placement_withdrawal_date,
    CASE
      WHEN pr.id IS NULL THEN apa.withdrawal_reason
      ELSE pr.withdrawal_reason
    END AS request_for_placement_withdrawal_reason,
    raw_applications_report.*
    
  FROM placement_applications_automatic paa
  
    INNER JOIN approved_premises_applications apa ON apa.id = paa.application_id 
    INNER JOIN raw_applications_report ON raw_applications_report.application_id = paa.application_id
    INNER JOIN applications a ON a.id = apa.id
    LEFT OUTER JOIN (
      SELECT DISTINCT ON (application_id)
             assessments.*
      FROM assessments
      WHERE reallocated_at IS NULL
      ORDER BY application_id, created_at DESC
    ) latest_assessment on latest_assessment.application_id = a.id
    LEFT OUTER JOIN placement_requests pr ON pr.application_id = a.id AND 
                    pr.reallocated_at IS NULL AND 
                    pr.placement_application_id IS NULL
    LEFT OUTER JOIN domain_events AS apa_withdrawn_event ON 
                    apa_withdrawn_event.application_id = apa.id AND
                    apa_withdrawn_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
    LEFT OUTER JOIN domain_events AS pr_withdrawn_event ON 
                    pr_withdrawn_event.application_id = apa.id AND
                    pr_withdrawn_event.type = 'APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN' AND 
                    cast(pr_withdrawn_event.data -> 'eventDetails' ->> 'matchRequestId' AS uuid) = pr.id
                    
  WHERE 
    apa.arrival_date IS NOT NULL
    AND
    ($placementRequestsRangeConstraints)
       
UNION ALL
       
  SELECT 
    null AS internal_placement_request_id,
    pa_date.id AS internal_placement_application_date_id,
    CONCAT('placement_application:',pa.id) AS request_for_placement_id,
    CASE
      WHEN pa.placement_type = '0' THEN 'ROTL'
      WHEN pa.placement_type = '1' THEN 'RELEASE_FOLLOWING_DECISION'
      WHEN pa.placement_type = '2' THEN 'ADDITIONAL_PLACEMENT'
      ELSE ''
    END AS request_for_placement_type, 
    to_char(pa_date.expected_arrival, 'YYYY-MM-DD') AS requested_arrival_date,
    pa_date.duration AS requested_duration_days,
    to_char(CAST(pa.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_submitted_date,
    to_char(CAST(pa.data -> 'request-a-placement' -> 'decision-to-release' ->> 'decisionToReleaseDate' as timestamp), 'YYYY-MM-DD') AS parole_decision_date,
    pa.decision AS request_for_placement_decision,
    to_char(CAST(pa.decision_made_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_decision_made_date,
    to_char(withdrawn_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_withdrawal_date,
    pa.withdrawal_reason AS request_for_placement_withdrawal_reason,
    raw_applications_report.*
    
  FROM
  
    placement_applications pa
    INNER JOIN raw_applications_report ON raw_applications_report.application_id = pa.application_id
    INNER JOIN placement_application_dates pa_date ON pa.id = pa_date.placement_application_id
    INNER JOIN approved_premises_applications apa ON pa.application_id = apa.id
    LEFT OUTER JOIN (
      SELECT DISTINCT ON (m.value)
             d.occurred_at,
             m.value as placement_application_id
      FROM domain_events d
      INNER JOIN domain_events_metadata m ON m.domain_event_id = d.id AND m.name = 'CAS1_PLACEMENT_APPLICATION_ID'
      WHERE d.type = 'APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN'
    ) withdrawn_event ON withdrawn_event.placement_application_id = CAST(pa.id as text)
    
  WHERE
    pa.submitted_at IS NOT NULL AND
    pa.reallocated_at IS NULL AND
    ($placementApplicationsRangeConstraints)
    ORDER BY request_for_placement_submitted_date ASC     
"""
    return s
  }

  val query = buildQuery(
    placementRequestsRangeConstraints = """
      (paa.submitted_at >= :startDateTimeInclusive AND paa.submitted_at <= :endDateTimeInclusive) OR
      (pr_withdrawn_event.occurred_at >= :startDateTimeInclusive AND pr_withdrawn_event.occurred_at <= :endDateTimeInclusive)
    """.trimIndent(),
    placementApplicationsRangeConstraints = """
      (pa.submitted_at >= :startDateTimeInclusive AND pa.submitted_at <= :endDateTimeInclusive) OR
      (withdrawn_event.occurred_at >= :startDateTimeInclusive AND withdrawn_event.occurred_at <= :endDateTimeInclusive)
    """.trimIndent(),
  ) + ";"

  fun generateForSubmissionOrWithdrawalDate(
    startDateTimeInclusive: LocalDateTime,
    endDateTimeInclusive: LocalDateTime,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    query,
    mapOf<String, Any>(
      "startDateTimeInclusive" to startDateTimeInclusive,
      "endDateTimeInclusive" to endDateTimeInclusive,
    ),
    jbdcResultSetConsumer,
  )
}
