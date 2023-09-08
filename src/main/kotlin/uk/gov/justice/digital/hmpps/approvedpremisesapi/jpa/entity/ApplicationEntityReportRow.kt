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
    SELECT
      CAST(application.id AS TEXT) as id,
      application.crn as crn,
      assessment.submitted_at as applicationAssessedDate,
      assessor_region.name as assessorCru,
      assessment.decision as assessmentDecision,
      assessment.rejection_rationale as assessmentDecisionRationale,
      CAST(ap_application.risk_ratings -> 'mappa' -> 'value' -> 'level' as TEXT) as mappa,
      ap_application.offence_id as offenceId,
      application.noms_number as noms,
      requirements.ap_type as premisesType,
      ap_application.release_type as releaseType,
      application.submitted_at as applicationSubmissionDate,
      referrer_region.name as referrerRegion,
      postcode_district.outcode as targetLocation,
      ap_application.withdrawal_reason as applicationWithdrawalReason,
      CAST(booking.id AS text) as bookingID,
      cancellation_reason.name as bookingCancellationReason,
      cancellation.date as bookingCancellationDate,
      booking.arrival_date as expectedArrivalDate,
      booking.departure_date as expectedDepartureDate,
      premises.name as premisesName,
      arrival.arrival_date as actualArrivalDate,
      departure.date_time as actualDepartureDate,
      move_on_category.name as departureMoveOnCategory,
      non_arrival.date as nonArrivalDate
    from
      applications application
      left join approved_premises_applications ap_application ON application.id = ap_application.id
      left join assessments assessment ON (
        application.id = assessment.application_id
        AND assessment.reallocated_at IS NULL
      )
      left join users referrer ON application.created_by_user_id = referrer.id
      left join users assessor ON assessment.allocated_to_user_id = assessor.id
      left join probation_regions assessor_region ON assessor.probation_region_id = assessor_region.id
      left join probation_regions referrer_region ON referrer.probation_region_id = referrer_region.id
      left join placement_requirements requirements ON application.id = requirements.application_id
      left join postcode_districts postcode_district ON requirements.postcode_district_id = postcode_district.id
      left join bookings booking ON booking.application_id = application.id
      left join cancellations cancellation ON booking.id = cancellation.booking_id
      left join cancellation_reasons cancellation_reason ON cancellation_reason.id = cancellation.cancellation_reason_id
      left join premises on booking.premises_id = premises.id
      left join arrivals arrival on arrival.booking_id = booking.id
      left join departures departure on departure.booking_id = booking.id
      left join move_on_categories move_on_category on departure.move_on_category_id = move_on_category.id
      left join non_arrivals non_arrival on non_arrival.booking_id = booking.id
    where
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
    """,
    nativeQuery = true,
  )
  fun generateApprovedPremisesReportRowsForCalendarMonth(month: Int, year: Int): List<ApplicationEntityReportRow>
}

interface ApplicationEntityReportRow {
  fun getId(): String
  fun getCrn(): String
  fun getApplicationAssessedDate(): Timestamp?
  fun getAssessorCru(): String?
  fun getAssessmentDecision(): String?
  fun getAssessmentDecisionRationale(): String?
  fun getMappa(): String?
  fun getOffenceId(): String
  fun getNoms(): String
  fun getPremisesType(): String?
  fun getReleaseType(): String?
  fun getApplicationSubmissionDate(): Timestamp?
  fun getReferrerRegion(): String?
  fun getTargetLocation(): String?
  fun getApplicationWithdrawalReason(): String?
  fun getBookingID(): String?
  fun getBookingCancellationReason(): String?
  fun getBookingCancellationDate(): Date?
  fun getExpectedArrivalDate(): Date?
  fun getExpectedDepartureDate(): Date?
  fun getPremisesName(): String?
  fun getActualArrivalDate(): Date?
  fun getActualDepartureDate(): Timestamp?
  fun getDepartureMoveOnCategory(): String?
  fun getNonArrivalDate(): Date?
}
