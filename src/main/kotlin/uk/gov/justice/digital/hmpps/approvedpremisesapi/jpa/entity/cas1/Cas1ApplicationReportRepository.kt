package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1ApplicationReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {

    const val COMPLETE_DATASET_QUERY = """
    SELECT DISTINCT on (application.submitted_at, application.id)
application.id as application_id,
submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' as crn,
submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' as noms,
submission_event.data -> 'eventDetails' ->> 'age' as age_in_years,
submission_event.data -> 'eventDetails' ->> 'gender' as gender,
'not yet provided' as ethnicity,
'not yet provided' as nationality,
'not yet provided' as religion,
'not yet provided' as has_physical_disability,
'not yet provided' as has_learning_social_communication_difficulty,
'not yet provided' as has_mental_health_condition,
apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
submission_event.data -> 'eventDetails' ->> 'mappa' as mappa,
submission_event.data -> 'eventDetails' ->> 'offenceId' as offence_id,
submission_ap_type_metadata.value as premises_type,
apa.sentence_type as sentence_type,
submission_event.data -> 'eventDetails' ->> 'releaseType' as release_type,
ap_area.name as application_origin_cru,
submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'ldu' ->> 'name' as referral_ldu,
submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'region' ->> 'name' as referral_region,
submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'team' ->> 'name' as referral_team,
submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'username' as referrer_username,
concat(
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'forenames', 
  ' ',
  submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'surname'
) as referrer_name,
submission_event.data -> 'eventDetails' ->> 'targetLocation' as target_location,
to_char(cast(submission_event.data -> 'eventDetails' ->> 'submittedAt' as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as application_submission_date,
apa.notice_type as application_timeliness_status,
reason_for_short_notice_metadata.value as applicant_reason_for_late_application,
reason_for_short_notice_other_metadata.value as applicant_reason_for_late_application_detail,
initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReason' as initial_assessor_agree_with_short_notice_reason,
initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReasonComments' as initial_assessor_reason_for_late_application,
initial_assessment.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'reasonForLateApplication' as initial_assessor_reason_for_late_application_detail,
initial_assessment_ap_type_metadata.value as initial_assessor_premises_type,
to_char(initial_assessment.allocated_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as last_allocated_to_initial_assessor_date,
initial_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'cru' ->> 'name' as initial_assessor_cru,
initial_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'staffMember' ->> 'username' as initial_assessor_username,
concat(
  initial_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'staffMember' ->> 'forenames', 
  ' ',
  initial_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'staffMember' ->> 'surname'
) as initial_assessor_name,
to_char(initial_assessment_clarification_notes.created_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as initial_assessment_further_information_requested_on,
to_char(initial_assessment_clarification_notes.response_received_on, 'YYYY-MM-DD') as initial_assessment_further_information_received_at,
to_char(cast(initial_assessment_event.data -> 'eventDetails' ->> 'assessedAt' as timestamp), 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as initial_assessment_decision_date,
initial_assessment_event.data -> 'eventDetails' ->> 'decision' as initial_assessment_decision,
initial_assessment_event.data -> 'eventDetails' ->> 'decisionRationale' as initial_assessment_decision_rationale,
(select count(*) from appeals where appeals.application_id = application.id) as assessment_appeal_count,
latest_appeal.decision as last_appealed_assessment_decision,
to_char(latest_appeal.appeal_date, 'YYYY-MM-DD') as last_appealed_assessment_date,
to_char(latest_appeal_assessment.allocated_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as last_allocated_to_appealed_assessor_date,
latest_appeal_assessment_ap_type_metadata.value as last_allocated_to_appealed_assessor_premises_type,
latest_appeal_assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'staffMember' ->> 'username' as last_appealed_assessor_username,
to_char(withdrawal_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as application_withdrawal_date,
withdrawal_event.data -> 'eventDetails' ->> 'withdrawalReason' as application_withdrawal_reason
from
applications application
inner join approved_premises_applications apa on application.id = apa.id
inner join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
 and application.id = submission_event.application_id
left join domain_events_metadata submission_ap_type_metadata on submission_ap_type_metadata.domain_event_id = submission_event.id 
 and submission_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE'  
 
left outer join domain_events_metadata reason_for_short_notice_metadata on reason_for_short_notice_metadata.domain_event_id = submission_event.id 
 and reason_for_short_notice_metadata.name = 'CAS1_APP_REASON_FOR_SHORT_NOTICE' 
left outer join domain_events_metadata reason_for_short_notice_other_metadata on reason_for_short_notice_other_metadata.domain_event_id = submission_event.id 
 and reason_for_short_notice_other_metadata.name = 'CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER'  
left join domain_events withdrawal_event on withdrawal_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
 and application.id = withdrawal_event.application_id
 
left join (
  select distinct on (application_id)
         assessments.*
  from assessments
  where reallocated_at IS NULL
  order by application_id, created_at asc
) initial_assessment on initial_assessment.application_id = application.id

left join domain_events as initial_assessment_event on 
  initial_assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
  initial_assessment_event.assessment_id = initial_assessment.id
left join domain_events_metadata initial_assessment_ap_type_metadata on initial_assessment_ap_type_metadata.domain_event_id = initial_assessment_event.id 
 and initial_assessment_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE' 
 
left join (
  select distinct on (assessment_id)
          assessment_clarification_notes.*
  from assessment_clarification_notes
  order by assessment_id, created_at asc
) initial_assessment_clarification_notes on initial_assessment_clarification_notes.assessment_id = initial_assessment.id

left join (
  select distinct on (application_id)
         appeals.*
  from appeals
  order by application_id, created_at desc
) latest_appeal on latest_appeal.application_id = application.id

left join (
  select distinct on (application_id)
         assess.*
  from assessments assess inner join approved_premises_assessments apa_assess on assess.id = apa_assess.assessment_id
  where assess.reallocated_at IS NULL and apa_assess.created_from_appeal is true
  order by assess.application_id, assess.created_at desc
) latest_appeal_assessment on latest_appeal_assessment.application_id = application.id

left join domain_events as latest_appeal_assessment_event on 
  latest_appeal_assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED' AND
  latest_appeal_assessment_event.assessment_id = latest_appeal_assessment.id
left join domain_events_metadata latest_appeal_assessment_ap_type_metadata on latest_appeal_assessment_ap_type_metadata.domain_event_id = latest_appeal_assessment_event.id 
 and latest_appeal_assessment_ap_type_metadata.name = 'CAS1_REQUESTED_AP_TYPE' 

left join users as latest_appeal_assessment_assessor on latest_appeal_assessment_assessor.id = latest_appeal_assessment.allocated_to_user_id
left join ap_areas as ap_area on ap_area.id = apa.ap_area_id

where application.service = 'approved-premises' AND
      application.submitted_at IS NOT NULL
    """

    const val QUERY =
      """
$COMPLETE_DATASET_QUERY
AND (
  (application.submitted_at >= :startDateTimeInclusive AND application.submitted_at <= :endDateTimeInclusive) OR
  (withdrawal_event.occurred_at >= :startDateTimeInclusive AND withdrawal_event.occurred_at <= :endDateTimeInclusive)
)
order by application.submitted_at ASC, application.id ASC 
;
"""
  }

  fun generateForSubmissionOrWithdrawalDate(
    startDateTimeInclusive: LocalDateTime,
    endDateTimeInclusive: LocalDateTime,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) =
    reportJdbcTemplate.query(
      QUERY,
      mapOf<String, Any>(
        "startDateTimeInclusive" to startDateTimeInclusive,
        "endDateTimeInclusive" to endDateTimeInclusive,
      ),
      jbdcResultSetConsumer,
    )
}
