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
  WITH rfps AS (
   SELECT 
    pr.id AS internal_placement_request_id,
    pap.id AS request_for_placement_id, 
    'STANDARD' AS request_for_placement_type,
    to_char(pap.expected_arrival_date, 'YYYY-MM-DD') AS requested_arrival_date,
    apa.requested_placement_duration AS requested_duration,
    to_char(CAST(a.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_submitted_date,
    null AS parole_decision_date,
    to_char(CAST(latest_assessment.allocated_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_last_allocated_to_assessor_date,
    latest_assessment.decision AS request_for_placement_decision,
    pr.duration AS authorised_duration,
    to_char(CAST(latest_assessment.submitted_at as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_decision_made_date,
    CASE
      WHEN pr.id IS NULL THEN to_char(apa_withdrawn_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
      ELSE to_char(pr.withdrawal_occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
     END AS request_for_placement_withdrawal_date,
    CASE
      WHEN pr.id IS NULL THEN apa.withdrawal_reason
      ELSE pr.withdrawal_reason
    END AS request_for_placement_withdrawal_reason,
    apa.sentence_type AS apa_sentence_type,
    CASE 
      WHEN apa.release_type = 'inCommunity' THEN apa.situation
      ELSE apa.release_type
    END AS placement_release_type,
    apa.id as internal_application_id
    FROM placement_applications_placeholder pap
  
    INNER JOIN approved_premises_applications apa ON apa.id = pap.application_id 
    INNER JOIN applications a ON a.id = apa.id
    LEFT JOIN (
      SELECT DISTINCT ON (application_id)
             *
      FROM assessments
      WHERE reallocated_at IS NULL
      ORDER BY application_id, created_at DESC
    ) latest_assessment ON latest_assessment.application_id = a.id
    
    LEFT OUTER JOIN placement_requests pr ON pr.application_id = a.id AND 
                    pr.reallocated_at IS NULL AND 
                    pr.placement_application_id IS NULL
    LEFT OUTER JOIN domain_events AS apa_withdrawn_event ON 
                    apa_withdrawn_event.application_id = apa.id AND
                    apa_withdrawn_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
                    
    WHERE 
      pap.archived is FALSE AND
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
      to_char(pa.withdrawal_occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS request_for_placement_withdrawal_date,
      pa.withdrawal_reason AS request_for_placement_withdrawal_reason,
      pa.sentence_type AS apa_sentence_type,
      CASE
        WHEN pa.release_type = 'inCommunity' THEN pa.situation
        ELSE pa.release_type
      END AS placement_release_type,
      apa.id as internal_application_id
    FROM
    
      placement_applications pa
      INNER JOIN approved_premises_applications apa ON pa.application_id = apa.id
      LEFT OUTER JOIN placement_requests pr ON pr.placement_application_id = pa.id AND pr.reallocated_at IS NULL

    WHERE
      pa.submitted_at IS NOT NULL AND
      pa.reallocated_at IS NULL AND
      ($placementApplicationsRangeConstraints)
  )
  ${Cas1ApplicationV2ReportRepository.applicationsDataSetQuery(
      selectAdditions = """
      DISTINCT ON (request_for_placement_submitted_date,request_for_placement_id,application.id)
      rfps.*,
      """.trimIndent(),
      joinFilter = """
      INNER JOIN rfps on rfps.internal_application_id = apa.id
      """.trimIndent(),
      whereAdditions = """
      ORDER BY request_for_placement_submitted_date,request_for_placement_id,application.id ASC  
      """.trimIndent(),
    )}     
"""
    return s
  }

  val query = buildQuery(
    placementRequestsRangeConstraints = """
      (pap.submitted_at >= :startDateTimeInclusive AND pap.submitted_at <= :endDateTimeInclusive) OR
      (pr.withdrawal_occurred_at >= :startDateTimeInclusive AND pr.withdrawal_occurred_at <= :endDateTimeInclusive)
    """.trimIndent(),
    placementApplicationsRangeConstraints = """
      (pa.submitted_at >= :startDateTimeInclusive AND pa.submitted_at <= :endDateTimeInclusive) OR
      (pa.withdrawal_occurred_at >= :startDateTimeInclusive AND pa.withdrawal_occurred_at <= :endDateTimeInclusive)
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
