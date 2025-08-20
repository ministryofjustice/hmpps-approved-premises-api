package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting

import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class Cas1RequestForPlacementReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  /**
   Because of the bifurcated placement request model, this query is a union from two sources:

   1. placement_applications_placeholder

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
    pap.id AS request_for_placement_id, 
    'STANDARD' AS request_for_placement_type,
    to_char(pap.expected_arrival_date, 'YYYY-MM-DD') AS requested_arrival_date,
    apa.duration AS requested_duration,
    to_char(CAST(a.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_submitted_date,
    null AS parole_decision_date,
    to_char(CAST(latest_assessment.allocated_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_last_allocated_to_assessor_date,
    latest_assessment.decision AS request_for_placement_decision,
    pr.duration AS authorised_duration,
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
    
  FROM placement_applications_placeholder pap
  
    INNER JOIN approved_premises_applications apa ON apa.id = pap.application_id 
    INNER JOIN raw_applications_report ON raw_applications_report.application_id = pap.application_id
    INNER JOIN applications a ON a.id = apa.id
    LEFT OUTER JOIN LATERAL (
      SELECT assessments.*
      FROM assessments
      WHERE application_id = a.id AND reallocated_at IS NULL
      ORDER BY created_at DESC
      LIMIT 1
    ) latest_assessment on TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery
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
    pap.archived is FALSE AND
    apa.arrival_date IS NOT NULL
    AND
    ($placementRequestsRangeConstraints)
       
UNION ALL
       
  SELECT 
    pr.id AS internal_placement_request_id,
    pa.id AS request_for_placement_id,
    CASE
      WHEN pa.placement_type = '0' THEN 'ROTL'
      WHEN pa.placement_type = '1' THEN 'RELEASE_FOLLOWING_DECISION'
      WHEN pa.placement_type = '2' THEN 'ADDITIONAL_PLACEMENT'
      WHEN pa.placement_type = '3' THEN 'STANDARD'
      ELSE ''
    END AS request_for_placement_type, 
    to_char(pa.expected_arrival, 'YYYY-MM-DD') AS requested_arrival_date,
    pa.requested_duration AS requested_duration,
    to_char(CAST(pa.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_submitted_date,
    to_char(CAST(pa.data -> 'request-a-placement' -> 'decision-to-release' ->> 'decisionToReleaseDate' as timestamp), 'YYYY-MM-DD') AS parole_decision_date,
    to_char(CAST(pa.allocated_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_last_allocated_to_assessor_date,
    pa.decision AS request_for_placement_decision,
    pa.authorised_duration AS authorised_duration,
    to_char(CAST(pa.decision_made_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_decision_made_date,
    to_char(withdrawn_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_withdrawal_date,
    pa.withdrawal_reason AS request_for_placement_withdrawal_reason,
    raw_applications_report.*
    
  FROM
  
    placement_applications pa
    INNER JOIN raw_applications_report ON raw_applications_report.application_id = pa.application_id
    INNER JOIN approved_premises_applications apa ON pa.application_id = apa.id
    LEFT OUTER JOIN placement_requests pr ON pr.placement_application_id = pa.id AND pr.reallocated_at IS NULL
    LEFT OUTER JOIN LATERAL (
      SELECT d.occurred_at,
             m.value as placement_application_id
      FROM domain_events d
      INNER JOIN domain_events_metadata m ON m.domain_event_id = d.id AND m.name = 'CAS1_PLACEMENT_APPLICATION_ID'
      WHERE
        d.application_id = pa.application_id AND
      	d.type = 'APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN' 
      	AND m.value = CAST(pa.id as text)
      LIMIT 1
    ) withdrawn_event ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery
    
  WHERE
    pa.submitted_at IS NOT NULL AND
    pa.reallocated_at IS NULL AND
    ($placementApplicationsRangeConstraints)
    ORDER BY request_for_placement_submitted_date,request_for_placement_id ASC     
"""
    return s
  }

  val query = buildQuery(
    placementRequestsRangeConstraints = """
      (pap.submitted_at >= :startDateTimeInclusive AND pap.submitted_at <= :endDateTimeInclusive) OR
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
