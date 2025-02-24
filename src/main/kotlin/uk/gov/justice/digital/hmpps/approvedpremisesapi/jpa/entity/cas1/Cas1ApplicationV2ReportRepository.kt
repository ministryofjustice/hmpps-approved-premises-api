package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1ApplicationV2ReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {

    const val COMPLETE_DATASET_QUERY = """
SELECT DISTINCT ON (application.submitted_at, application.id)
  application.id AS application_id,
  submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' AS crn,
  submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' AS noms,
  submission_event.data -> 'eventDetails' ->> 'age' AS age_in_years,
  submission_event.data -> 'eventDetails' ->> 'gender' AS gender,
  'not yet provided' AS ethnicity,
  'not yet provided' AS nationality,
  'not yet provided' AS religion,
  'not yet provided' AS has_physical_disability,
  'not yet provided' AS has_learning_social_communication_difficulty,
  'not yet provided' AS has_mental_health_condition,
  apa.risk_ratings -> 'tier' -> 'value' ->> 'level' AS tier,
  submission_event.data -> 'eventDetails' ->> 'mappa' AS mappa,
  submission_event.data -> 'eventDetails' ->> 'offenceId' AS offence_id,
  submission_ap_type_metadata.value AS premises_type,
  apa.sentence_type AS sentence_type,
  submission_event.data -> 'eventDetails' ->> 'releaseType' AS release_type,
  ap_area.name AS application_origin_cru,
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'ldu' ->> 'name' AS referral_ldu,
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'region' ->> 'name' AS referral_region,
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'team' ->> 'name' AS referral_team,
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'username' AS referrer_username,
  concat(
    submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'forenames', 
    ' ',
    submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'surname'
  ) AS referrer_name,
  submission_event.data -> 'eventDetails' ->> 'targetLocation' AS target_location,
  to_char(apa.arrival_date, 'YYYY-MM-DD') AS standard_rfp_arrival_date,
  to_char(cast(submission_event.data -> 'eventDetails' ->> 'submittedAt' AS timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS application_submission_date,
  apa.notice_type AS application_timeliness_status,
  reason_for_short_notice_metadata.value AS applicant_reason_for_late_application,
  reason_for_short_notice_other_metadata.value AS applicant_reason_for_late_application_detail,
  initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReason' AS initial_assessor_agree_with_short_notice_reason,
  initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReasonComments' AS initial_assessor_reason_for_late_application,
  initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'reasonForLateApplication' AS initial_assessor_reason_for_late_application_detail,
  initial_assessment_ap_type_metadata.value AS initial_assessor_premises_type,
  to_char(initial_assessment.allocated_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS last_allocated_to_initial_assessor_date,
  initial_assessor_area.name as initial_assessor_cru,
  initial_assessor.delius_username as initial_assessor_username,
  initial_assessor.name as initial_assessor_name,
  to_char(initial_assessment_clarification_notes.created_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS initial_assessment_further_information_requested_on,
  to_char(initial_assessment_clarification_notes.response_received_on, 'YYYY-MM-DD') AS initial_assessment_further_information_received_at,
  to_char(cast(initial_assessment_event.data -> 'eventDetails' ->> 'assessedAt' as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS initial_assessment_decision_date,
  initial_assessment_event.data -> 'eventDetails' ->> 'decision' AS initial_assessment_decision,
  initial_assessment_event.data -> 'eventDetails' ->> 'decisionRationale' AS initial_assessment_decision_rationale,
  (select count(*) from appeals where appeals.application_id = application.id) AS assessment_appeal_count,
  latest_appeal.decision AS last_appealed_assessment_decision,
  to_char(latest_appeal.appeal_date, 'YYYY-MM-DD') AS last_appealed_assessment_date,
  to_char(latest_appeal_assessment.allocated_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS last_allocated_to_appealed_assessor_date,
  latest_appeal_assessment_ap_type_metadata.value AS last_allocated_to_appealed_assessor_premises_type,
  latest_appeal_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'staffMember' ->> 'username' AS last_appealed_assessor_username,
  to_char(withdrawal_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS application_withdrawal_date,
  withdrawal_event.data -> 'eventDetails' ->> 'withdrawalReason' AS application_withdrawal_reason
  

FROM approved_premises_applications apa

INNER JOIN applications application on application.id = apa.id

INNER JOIN domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED' 
  AND application.id = submission_event.application_id
LEFT JOIN domain_events_metadata submission_ap_type_metadata on submission_ap_type_metadata.domain_event_id = submission_event.id 
  AND submission_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE'
 
LEFT OUTER JOIN domain_events_metadata reason_for_short_notice_metadata ON reason_for_short_notice_metadata.domain_event_id = submission_event.id 
  AND reason_for_short_notice_metadata.name = 'CAS1_APP_REASON_FOR_SHORT_NOTICE' 
LEFT OUTER JOIN domain_events_metadata reason_for_short_notice_other_metadata ON reason_for_short_notice_other_metadata.domain_event_id = submission_event.id 
  AND reason_for_short_notice_other_metadata.name = 'CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER'  
LEFT JOIN domain_events withdrawal_event on withdrawal_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
  AND application.id = withdrawal_event.application_id
 
LEFT OUTER JOIN LATERAL (
  SELECT assessments.*
  FROM assessments
  WHERE reallocated_at IS NULL AND application_id = application.id
  ORDER BY application_id, created_at ASC
  LIMIT 1
) initial_assessment ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery

LEFT JOIN users initial_assessor ON initial_assessor.id = initial_assessment.allocated_to_user_id
LEFT JOIN ap_areas initial_assessor_area ON initial_assessor_area.id = initial_assessor.ap_area_id

LEFT JOIN domain_events AS initial_assessment_event ON 
  initial_assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
  initial_assessment_event.assessment_id = initial_assessment.id
LEFT JOIN domain_events_metadata initial_assessment_ap_type_metadata on initial_assessment_ap_type_metadata.domain_event_id = initial_assessment_event.id 
  AND initial_assessment_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE' 
 
LEFT OUTER JOIN LATERAL (
  SELECT assessment_clarification_notes.*
  FROM assessment_clarification_notes
  WHERE assessment_id = initial_assessment.id
  ORDER BY assessment_id, created_at ASC
  LIMIT 1
) initial_assessment_clarification_notes ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery

LEFT OUTER JOIN LATERAL (
  SELECT appeals.*
  FROM appeals
  WHERE application_id = application.id
  ORDER BY application_id, created_at DESC
  LIMIT 1
) latest_appeal ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery

LEFT OUTER JOIN LATERAL (
  SELECT assess.*
  FROM assessments assess INNER JOIN approved_premises_assessments apa_assess ON assess.id = apa_assess.assessment_id
  WHERE assess.application_id = application.id AND assess.reallocated_at IS NULL AND apa_assess.created_from_appeal is TRUE
  ORDER by assess.application_id, assess.created_at DESC
  LIMIT 1
) latest_appeal_assessment ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery


LEFT JOIN domain_events as latest_appeal_assessment_event ON 
  latest_appeal_assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
  latest_appeal_assessment_event.assessment_id = latest_appeal_assessment.id
LEFT JOIN domain_events_metadata latest_appeal_assessment_ap_type_metadata ON latest_appeal_assessment_ap_type_metadata.domain_event_id = latest_appeal_assessment_event.id 
  AND latest_appeal_assessment_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE' 

LEFT JOIN users as latest_appeal_assessment_assessor on latest_appeal_assessment_assessor.id = latest_appeal_assessment.allocated_to_user_id
LEFT JOIN ap_areas as ap_area on ap_area.id = apa.ap_area_id

WHERE application.service = 'approved-premises' AND
      application.submitted_at IS NOT NULL
    """

    const val QUERY =
      """
$COMPLETE_DATASET_QUERY
AND (
  (application.submitted_at >= :startDateTimeInclusive AND application.submitted_at <= :endDateTimeInclusive) OR
  (withdrawal_event.occurred_at >= :startDateTimeInclusive AND withdrawal_event.occurred_at <= :endDateTimeInclusive)
)
ORDER BY application.submitted_at ASC, application.id ASC 
;
"""
  }

  fun generateForSubmissionOrWithdrawalDate(
    startDateTimeInclusive: LocalDateTime,
    endDateTimeInclusive: LocalDateTime,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    QUERY,
    mapOf<String, Any>(
      "startDateTimeInclusive" to startDateTimeInclusive,
      "endDateTimeInclusive" to endDateTimeInclusive,
    ),
    jbdcResultSetConsumer,
  )
}
