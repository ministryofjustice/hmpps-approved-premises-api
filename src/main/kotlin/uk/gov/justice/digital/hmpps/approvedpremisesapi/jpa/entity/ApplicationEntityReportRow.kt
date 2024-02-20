package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.util.UUID

@Repository
interface ApplicationEntityReportRowRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query(
    """
    SELECT DISTINCT
      cast(application.id as TEXT) as id,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' as crn,
      assessments.allocated_at as lastAllocatedToAssessorDate,
      cast(assessment_event.data -> 'eventDetails' ->> 'assessedAt' as date) as applicationAssessedDate,
      assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'cru' ->> 'name' as assessorCru,
      assessment_event.data -> 'eventDetails' ->> 'decision' as assessmentDecision,
      assessment_event.data -> 'eventDetails' ->> 'decisionRationale' as assessmentDecisionRationale,
      submission_event.data -> 'eventDetails' ->> 'age' as ageInYears,
      submission_event.data -> 'eventDetails' ->> 'gender' as gender,
      submission_event.data -> 'eventDetails' ->> 'mappa' as mappa,
      submission_event.data -> 'eventDetails' ->> 'offenceId' as offenceId,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' as noms,
      (
        CASE
          WHEN apa.is_pipe_application THEN 'pipe'
          WHEN apa.is_esap_application THEN 'esap'
          ELSE 'normal'
        END
      ) as premisesType,
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
      cast(booking_made_event.booking_id as text) as bookingID,
      booking_made_event.occurred_at as bookingMadeDate,
      booking_cancelled_event.data -> 'eventDetails' ->> 'cancellationReason' as bookingCancellationReason,
      cast(booking_cancelled_event.data -> 'eventDetails' ->> 'cancelledAt' as date) as bookingCancellationDate,
      cast(booking_made_event.data -> 'eventDetails' ->> 'arrivalOn' as date) as expectedArrivalDate,
      booking_made_event.data -> 'eventDetails' -> 'bookedBy' -> 'cru' ->> 'name' as matcherCru,
      cast(booking_made_event.data -> 'eventDetails' ->> 'departureOn' as date) as expectedDepartureDate,
      booking_made_event.data -> 'eventDetails' -> 'premises' ->> 'name' as premisesName,
      cast(arrival_event.data -> 'eventDetails' ->> 'arrivedAt' as date) as actualArrivalDate,
      cast(departure_event.data -> 'eventDetails' ->> 'departedAt' as date) as actualDepartureDate,
      departure_event.data -> 'eventDetails' ->> 'reason' as departureReason,
      departure_event.data -> 'eventDetails' -> 'destination' -> 'moveOnCategory' ->> 'description' as departureMoveOnCategory,
      non_arrival_event.data IS NOT NULL as hasNotArrived,
      non_arrival_event.data -> 'eventDetails' ->> 'reason' as notArrivedReason,
      cast(placement_applications.data -> 'request-a-placement' -> 'decision-to-release' ->> 'decisionToReleaseDate' as date) as paroleDecisionDate,
      (
        CASE
          WHEN placement_applications.id IS NULL THEN 'referral'
          ELSE 'placement request'
        END
      ) as type
    from
      applications application
      left join approved_premises_applications apa on application.id = apa.id
      left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
      and application.id = submission_event.application_id
      left join domain_events assessment_event on assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
      and application.id = assessment_event.application_id
      left join domain_events withdrawl_event on withdrawl_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
      and application.id = withdrawl_event.application_id
      left join domain_events booking_made_event on booking_made_event.type = 'APPROVED_PREMISES_BOOKING_MADE'
      and application.id = booking_made_event.application_id
      left join domain_events booking_cancelled_event on booking_cancelled_event.type = 'APPROVED_PREMISES_BOOKING_CANCELLED'
      and application.id = booking_cancelled_event.application_id
      left join domain_events arrival_event on arrival_event.type = 'APPROVED_PREMISES_PERSON_ARRIVED'
      and application.id = arrival_event.application_id
      left join domain_events departure_event on departure_event.type = 'APPROVED_PREMISES_PERSON_DEPARTED'
      and application.id = departure_event.application_id
      left join domain_events non_arrival_event on non_arrival_event.type = 'APPROVED_PREMISES_PERSON_NOT_ARRIVED'
      and application.id = non_arrival_event.application_id
      left join assessments on application.id = assessments.application_id AND assessments.reallocated_at IS NULL
      left join placement_requests on placement_requests.booking_id = booking_made_event.booking_id
      left join placement_applications on placement_applications.id = placement_requests.placement_application_id
    where
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
      AND application.service = 'approved-premises';
    """,
    nativeQuery = true,
  )
  fun generateApprovedPremisesReportRowsForCalendarMonth(month: Int, year: Int): List<ApplicationEntityReportRow>

  @Query(
    """
    SELECT distinct
      on (application.id) cast(application.id as TEXT) as id,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'crn' as crn,
      assessments.allocated_at as lastAllocatedToAssessorDate,
      cast(
        assessment_event.data -> 'eventDetails' ->> 'assessedAt' as date
      ) as applicationAssessedDate,
      assessment_event.data -> 'eventDetails' -> 'assessedBy' -> 'cru' ->> 'name' as assessorCru,
      assessment_event.data -> 'eventDetails' ->> 'decision' as assessmentDecision,
      assessment_event.data -> 'eventDetails' ->> 'decisionRationale' as assessmentDecisionRationale,
      submission_event.data -> 'eventDetails' ->> 'age' as ageInYears,
      submission_event.data -> 'eventDetails' ->> 'gender' as gender,
      submission_event.data -> 'eventDetails' ->> 'mappa' as mappa,
      submission_event.data -> 'eventDetails' ->> 'offenceId' as offenceId,
      submission_event.data -> 'eventDetails' -> 'personReference' ->> 'noms' as noms,
      (
        CASE
          WHEN apa.is_pipe_application THEN 'pipe'
          WHEN apa.is_esap_application THEN 'esap'
          ELSE 'normal'
        END
      ) as premisesType,
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
      cast(booking_made_event.booking_id as text) as bookingID,
      booking_made_event.occurred_at as bookingMadeDate,
      booking_cancelled_event.data -> 'eventDetails' ->> 'cancellationReason' as bookingCancellationReason,
      cast(
        booking_cancelled_event.data -> 'eventDetails' ->> 'cancelledAt' as date
      ) as bookingCancellationDate,
      cast(
        booking_made_event.data -> 'eventDetails' ->> 'arrivalOn' as date
      ) as expectedArrivalDate,
      booking_made_event.data -> 'eventDetails' -> 'bookedBy' -> 'cru' ->> 'name' as matcherCru,
      cast(
        booking_made_event.data -> 'eventDetails' ->> 'departureOn' as date
      ) as expectedDepartureDate,
      booking_made_event.data -> 'eventDetails' -> 'premises' ->> 'name' as premisesName,
      cast(
        arrival_event.data -> 'eventDetails' ->> 'arrivedAt' as date
      ) as actualArrivalDate,
      cast(
        departure_event.data -> 'eventDetails' ->> 'departedAt' as date
      ) as actualDepartureDate,
      departure_event.data -> 'eventDetails' ->> 'reason' as departureReason,
      departure_event.data -> 'eventDetails' -> 'destination' -> 'moveOnCategory' ->> 'description' as departureMoveOnCategory,
      non_arrival_event.data IS NOT NULL as hasNotArrived,
      non_arrival_event.data -> 'eventDetails' ->> 'reason' as notArrivedReason
    from
      applications application
      left join approved_premises_applications apa on application.id = apa.id
      left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
      and application.id = submission_event.application_id
      left join domain_events assessment_event on assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
      and application.id = assessment_event.application_id
      left join domain_events withdrawl_event on withdrawl_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
      and application.id = withdrawl_event.application_id
      left join domain_events booking_made_event on booking_made_event.type = 'APPROVED_PREMISES_BOOKING_MADE'
      and application.id = booking_made_event.application_id
      left join domain_events booking_cancelled_event on booking_cancelled_event.type = 'APPROVED_PREMISES_BOOKING_CANCELLED'
      and application.id = booking_cancelled_event.application_id
      left join domain_events arrival_event on arrival_event.type = 'APPROVED_PREMISES_PERSON_ARRIVED'
      and application.id = arrival_event.application_id
      left join domain_events departure_event on departure_event.type = 'APPROVED_PREMISES_PERSON_DEPARTED'
      and application.id = departure_event.application_id
      left join domain_events non_arrival_event on non_arrival_event.type = 'APPROVED_PREMISES_PERSON_NOT_ARRIVED'
      and application.id = non_arrival_event.application_id
      left join assessments on application.id = assessments.application_id
      AND assessments.reallocated_at IS NULL
    where
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
      AND application.service = 'approved-premises'
    order by
      application.id,
      submission_event.occurred_at, 
      booking_made_event.occurred_at desc;
    """,
    nativeQuery = true,
  )
  fun generateApprovedPremisesReferralReportRowsForCalendarMonth(month: Int, year: Int): List<ApplicationEntityReportRow>
}

interface ApplicationEntityReportRow {
  fun getId(): String
  fun getCrn(): String
  fun getLastAllocatedToAssessorDate(): Timestamp?
  fun getApplicationAssessedDate(): Date?
  fun getAssessorCru(): String?
  fun getAssessmentDecision(): String?
  fun getAgeInYears(): String?
  fun getGender(): String?
  fun getAssessmentDecisionRationale(): String?
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
  fun getBookingID(): String?
  fun getBookingMadeDate(): Timestamp?
  fun getBookingCancellationReason(): String?
  fun getBookingCancellationDate(): Date?
  fun getExpectedArrivalDate(): Date?
  fun getExpectedDepartureDate(): Date?
  fun getMatcherCru(): String?
  fun getPremisesName(): String?
  fun getActualArrivalDate(): Date?
  fun getActualDepartureDate(): Date?
  fun getDepartureMoveOnCategory(): String?
  fun getDepartureReason(): String?
  fun getHasNotArrived(): Boolean?
  fun getNotArrivedReason(): String?
  fun getParoleDecisionDate(): Date?
  fun getType(): String?
}
