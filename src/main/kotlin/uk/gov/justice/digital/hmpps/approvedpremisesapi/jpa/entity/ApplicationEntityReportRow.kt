package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Date
import java.time.Instant
import java.util.UUID

@Repository
interface ApplicationEntityReportRowRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query(
    """
    SELECT DISTINCT on (application.id)
      cast(application.id as TEXT) as id,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' as crn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      assessments.allocated_at as lastAllocatedToAssessorDate,
      cast(assessment_event.data -> 'eventDetails' ->> 'assessedAt' as date) as applicationAssessedDate,
      assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'cru' ->> 'name' as assessorCru,
      assessment_event.data -> 'eventDetails' ->> 'decision' as assessmentDecision,
      assessment_event.data -> 'eventDetails' ->> 'decisionRationale' as assessmentDecisionRationale,
      application.data -> 'basic-information' -> 'reason-for-short-notice' ->> 'reason' as applicantReasonForLateApplication,
      application.data -> 'basic-information' -> 'reason-for-short-notice' ->> 'other' as applicantReasonForLateApplicationDetail,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReason' as assessorAgreeWithShortNoticeReason,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReasonComments' as assessorReasonForLateApplication,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'reasonForLateApplication' as assessorReasonForLateApplicationDetail,
      submission_event.data -> 'eventDetails' ->> 'age' as ageInYears,
      submission_event.data -> 'eventDetails' ->> 'gender' as gender,
      submission_event.data -> 'eventDetails' ->> 'mappa' as mappa,
      submission_event.data -> 'eventDetails' ->> 'offenceId' as offenceId,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' as noms,
      lower(apa.ap_type) as premisesType,
      application.data -> 'basic-information' -> 'sentence-type' ->> 'sentenceType' as sentenceType,
      submission_event.data -> 'eventDetails' ->> 'releaseType' as releaseType,
      cast(submission_event.data -> 'eventDetails' ->> 'submittedAt' as date) as applicationSubmissionDate,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'ldu' ->> 'name' as referralLdu,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'team' ->> 'name' as referralTeam,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'region' ->> 'name' as referralRegion,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'username' as referrerUsername,
      submission_event.data -> 'eventDetails' ->> 'targetLocation' as targetLocation,
      cast(withdrawl_event.data -> 'eventDetails' ->> 'withdrawnAt' as date) as applicationWithdrawalDate,
      withdrawl_event.data -> 'eventDetails' ->> 'withdrawalReason' as applicationWithdrawalReason,
      cast(apa.arrival_date as date) as expectedArrivalDate,
      (
        select pr.expected_arrival + pr.duration
        from placement_requests pr
        where pr.application_id = application.id AND
              pr.placement_application_id IS NULL AND
              pr.reallocated_at IS NULL
        limit 1       
      ) as expectedDepartureDate,
      (
        select count(*)
        from appeals
        where appeals.application_id = application.id
        and appeals.assessment_id = assessment_event.assessment_id
      ) as assessmentAppealCount,
      latest_appeal.decision as lastAssessmentAppealedDecision,
      latest_appeal.appeal_date as lastAssessmentAppealedDate,
      (
        select decision
        from assessments
        where assessments.id = assessment_event.assessment_id
      ) as assessmentAppealedFromStatus
    from
      applications application
      left join approved_premises_applications apa on application.id = apa.id
      left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
      and application.id = submission_event.application_id
      left join domain_events assessment_event on assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
      and application.id = assessment_event.application_id
      left join domain_events withdrawl_event on withdrawl_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
      and application.id = withdrawl_event.application_id
      left join assessments on application.id = assessments.application_id AND assessments.reallocated_at IS NULL
      left join (
        select distinct on (appeals.application_id, appeals.assessment_id)
          appeals.*
        from appeals
        order by appeals.application_id, appeals.assessment_id, created_at desc
      ) latest_appeal on latest_appeal.application_id = application.id
      and latest_appeal.assessment_id = assessment_event.assessment_id
    where
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
      AND application.service = 'approved-premises'
    order by application.id, latest_appeal.appeal_date desc nulls last;
    """,
    nativeQuery = true,
  )
  fun generateApprovedPremisesReportRowsForCalendarMonth(month: Int, year: Int): List<ApplicationEntityReportRow>

  @Query(
    """
    SELECT distinct
      on (application.id) cast(application.id as TEXT) as id,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' as crn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      assessments.allocated_at as lastAllocatedToAssessorDate,
      cast(
        assessment_event.data -> 'eventDetails' ->> 'assessedAt' as date
      ) as applicationAssessedDate,
      assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'cru' ->> 'name' as assessorCru,
      assessment_event.data -> 'eventDetails' ->> 'decision' as assessmentDecision,
      assessment_event.data -> 'eventDetails' ->> 'decisionRationale' as assessmentDecisionRationale,
      application.data -> 'basic-information' -> 'reason-for-short-notice' ->> 'reason' as applicantReasonForLateApplication,
      application.data -> 'basic-information' -> 'reason-for-short-notice' ->> 'other' as applicantReasonForLateApplicationDetail,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReason' as assessorAgreeWithShortNoticeReason,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReasonComments' as assessorReasonForLateApplication,
      assessments.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'reasonForLateApplication' as assessorReasonForLateApplicationDetail,
      submission_event.data -> 'eventDetails' ->> 'age' as ageInYears,
      submission_event.data -> 'eventDetails' ->> 'gender' as gender,
      submission_event.data -> 'eventDetails' ->> 'mappa' as mappa,
      submission_event.data -> 'eventDetails' ->> 'offenceId' as offenceId,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' as noms,
      lower(apa.ap_type) as premisesType,
      application.data -> 'basic-information' -> 'sentence-type' ->> 'sentenceType' as sentenceType,
      submission_event.data -> 'eventDetails' ->> 'releaseType' as releaseType,
      cast(
        submission_event.data -> 'eventDetails' ->> 'submittedAt' as date
      ) as applicationSubmissionDate,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'ldu' ->> 'name' as referralLdu,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'team' ->> 'name' as referralTeam,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'region' ->> 'name' as referralRegion,
      submission_event.data -> 'eventDetails' -> 'submittedBy' -> 'staffMember' ->> 'username' as referrerUsername,
      submission_event.data -> 'eventDetails' ->> 'targetLocation' as targetLocation,
      cast(
        withdrawl_event.data -> 'eventDetails' ->> 'withdrawnAt' as date
      ) as applicationWithdrawalDate,
      withdrawl_event.data -> 'eventDetails' ->> 'withdrawalReason' as applicationWithdrawalReason,
      cast(apa.arrival_date as date) as expectedArrivalDate,
      (
        select pr.expected_arrival + pr.duration
        from placement_requests pr
        where pr.application_id = application.id AND
              pr.placement_application_id IS NULL AND
              pr.reallocated_at IS NULL
        limit 1       
      ) as expectedDepartureDate,
      (
        select count(*)
        from appeals
        where appeals.application_id = application.id
        and appeals.assessment_id = assessment_event.assessment_id
      ) as assessmentAppealCount,
      latest_appeal.decision as lastAssessmentAppealedDecision,
      latest_appeal.appeal_date as lastAssessmentAppealedDate,
      (
        select decision
        from assessments
        where assessments.id = assessment_event.assessment_id
      ) as assessmentAppealedFromStatus
    from
      applications application
      left join approved_premises_applications apa on application.id = apa.id
      left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
      and application.id = submission_event.application_id
      left join domain_events assessment_event on assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
      and application.id = assessment_event.application_id
      left join domain_events withdrawl_event on withdrawl_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
      and application.id = withdrawl_event.application_id
      left join assessments on application.id = assessments.application_id
      AND assessments.reallocated_at IS NULL
      left join (
        select distinct on (appeals.application_id, appeals.assessment_id)
          appeals.*
        from appeals
        order by appeals.application_id, appeals.assessment_id, created_at desc
      ) latest_appeal on latest_appeal.application_id = application.id
    where
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
      AND application.service = 'approved-premises'
    order by
      application.id,
      submission_event.occurred_at, 
      latest_appeal.appeal_date desc nulls last;
    """,
    nativeQuery = true,
  )
  fun generateApprovedPremisesReferralReportRowsForCalendarMonth(month: Int, year: Int): List<ApplicationEntityReportRow>
}

interface ApplicationEntityReportRow {
  fun getId(): String
  fun getCrn(): String
  fun getTier(): String?
  fun getLastAllocatedToAssessorDate(): Instant?
  fun getApplicationAssessedDate(): Date?
  fun getAssessorCru(): String?
  fun getAssessmentDecision(): String?
  fun getAssessmentDecisionRationale(): String?
  fun getApplicantReasonForLateApplication(): String?
  fun getApplicantReasonForLateApplicationDetail(): String?
  fun getAssessorAgreeWithShortNoticeReason(): String?
  fun getAssessorReasonForLateApplication(): String?
  fun getAssessorReasonForLateApplicationDetail(): String?
  fun getAgeInYears(): String?
  fun getGender(): String?
  fun getMappa(): String?
  fun getOffenceId(): String
  fun getNoms(): String
  fun getPremisesType(): String?
  fun getSentenceType(): String?
  fun getReleaseType(): String?
  fun getApplicationSubmissionDate(): Date?
  fun getReferralRegion(): String?
  fun getReferralTeam(): String?
  fun getReferralLdu(): String?
  fun getReferrerUsername(): String?
  fun getTargetLocation(): String?
  fun getApplicationWithdrawalDate(): Date?
  fun getApplicationWithdrawalReason(): String?
  fun getExpectedArrivalDate(): Date?
  fun getExpectedDepartureDate(): Date?
  fun getAssessmentAppealCount(): Int?
  fun getLastAssessmentAppealedDecision(): String?
  fun getLastAssessmentAppealedDate(): Date?
  fun getAssessmentAppealedFromStatus(): String?
}
