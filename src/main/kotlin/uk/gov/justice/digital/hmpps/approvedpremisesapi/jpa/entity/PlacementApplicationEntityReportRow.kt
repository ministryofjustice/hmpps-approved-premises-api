package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.util.UUID

@Repository
interface PlacementApplicationEntityReportRowRepository : JpaRepository<PlacementApplicationEntity, UUID> {
  @Query(
    """
      select distinct on (pa.id, pa_dates.expected_arrival, pa_dates.duration, placement_request.booking_id)
        cast(pa.id as TEXT) as id,
        application.crn as crn,
        pa_dates.expected_arrival as requestedArrivalDate,
        pa_dates.duration as requestedDurationDays,
        pa.decision as decision,
        pa.decision_made_at as decisionMadeAt,
        pa.submitted_at as placementApplicationSubmittedAt,
        application.submitted_at as applicationSubmittedAt,
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
        non_arrival_event.data -> 'eventDetails' ->> 'reason' as notArrivedReason,
        pa.data -> 'request-a-placement' -> 'reason-for-placement' ->> 'reason' as placementRequestType,
        cast(
          pa.data -> 'request-a-placement' -> 'decision-to-release' ->> 'decisionToReleaseDate' as date
        ) as paroleDecisionDate,
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
        placement_applications pa
        left join applications application on pa.application_id = application.id
        left join approved_premises_applications apa on application.id = apa.id
        left join placement_application_dates pa_dates on pa_dates.placement_application_id = pa.id
        left join placement_requests placement_request on placement_request.id = pa_dates.placement_request_id
        left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
        and application.id = submission_event.application_id
        left join domain_events booking_made_event on booking_made_event.type = 'APPROVED_PREMISES_BOOKING_MADE'
        and placement_request.booking_id = booking_made_event.booking_id
        left join domain_events booking_cancelled_event on booking_cancelled_event.type = 'APPROVED_PREMISES_BOOKING_CANCELLED'
        and placement_request.booking_id = booking_cancelled_event.booking_id
        left join domain_events assessment_event on assessment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
        and application.id = assessment_event.application_id
        left join (
          select distinct on (appeals.application_id, appeals.assessment_id)
            appeals.*
          from appeals
          order by appeals.application_id, appeals.assessment_id, created_at desc
        ) latest_appeal on latest_appeal.application_id = application.id
        and latest_appeal.assessment_id = assessment_event.assessment_id
        left join domain_events withdrawl_event on withdrawl_event.type = 'APPROVED_PREMISES_APPLICATION_WITHDRAWN'
        and application.id = withdrawl_event.application_id
        left join domain_events arrival_event on arrival_event.type = 'APPROVED_PREMISES_PERSON_ARRIVED'
        and placement_request.booking_id = arrival_event.booking_id
        left join domain_events departure_event on departure_event.type = 'APPROVED_PREMISES_PERSON_DEPARTED'
        and placement_request.booking_id = departure_event.booking_id
        left join domain_events non_arrival_event on non_arrival_event.type = 'APPROVED_PREMISES_PERSON_NOT_ARRIVED'
        and placement_request.booking_id = non_arrival_event.booking_id
      where
        pa.reallocated_at is null
        AND pa.submitted_at is not null
        AND date_part('month', pa.submitted_at) = :month
        AND date_part('year', pa.submitted_at) = :year
        AND application.service = 'approved-premises'
      order by pa.id, pa_dates.expected_arrival, pa_dates.duration, placement_request.booking_id, latest_appeal.appeal_date desc nulls last;
    """,
    nativeQuery = true,
  )
  fun generatePlacementApplicationEntityReportRowsForCalendarMonth(month: Int, year: Int): List<PlacementApplicationEntityReportRow>
}

@Suppress("TooManyFunctions")
interface PlacementApplicationEntityReportRow {
  fun getId(): String
  fun getCrn(): String
  fun getRequestedArrivalDate(): Date?
  fun getRequestedDurationDays(): Int?
  fun getDecision(): String?
  fun getDecisionMadeAt(): Timestamp?
  fun getPlacementApplicationSubmittedAt(): Timestamp?
  fun getApplicationSubmittedAt(): Timestamp?
  fun getApplicationAssessedDate(): Date?
  fun getAssessorCru(): String?
  fun getAssessmentDecision(): String?
  fun getAssessmentDecisionRationale(): String?
  fun getAgeInYears(): String?
  fun getGender(): String?
  fun getMappa(): String?
  fun getOffenceId(): String
  fun getNoms(): String
  fun getSentenceType(): String?
  fun getReleaseType(): String?
  fun getReferralLdu(): String?
  fun getReferralTeam(): String?
  fun getReferralRegion(): String?
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
  fun getPremisesName(): String?
  fun getActualArrivalDate(): Date?
  fun getActualDepartureDate(): Date?
  fun getDepartureReason(): String?
  fun getDepartureMoveOnCategory(): String?
  fun getHasNotArrived(): Boolean?
  fun getNotArrivedReason(): String?
  fun getPlacementRequestType(): String?
  fun getParoleDecisionDate(): Date?
  fun getAssessmentAppealCount(): Int?
  fun getLastAssessmentAppealedDecision(): String?
  fun getLastAssessmentAppealedDate(): Date?
  fun getAssessmentAppealedFromStatus(): String?
}
