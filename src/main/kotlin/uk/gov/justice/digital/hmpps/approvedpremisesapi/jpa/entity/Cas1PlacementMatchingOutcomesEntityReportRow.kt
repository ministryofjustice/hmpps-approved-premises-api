package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Date
import java.util.UUID

@Repository
interface PlacementMatchingOutcomesEntityReportRowRepository : JpaRepository<PlacementApplicationEntity, UUID> {

  companion object {
    /**
     * We don't have a dedicated entity representing the dates requested
     * on the initial application (if any were requested at all).
     *
     * To find these dates we look for placement_requests that aren't linked to a
     * placement_application as this also includes withdrawal information
     *
     * For more information see [PlacementRequestEntity.isForApplicationsArrivalDate]
     */
    private const val INITIAL_REQUEST_FOR_PLACEMENT_QUERY = """
      SELECT 
        a.crn as crn,
        apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
        CAST(a.id as TEXT) as applicationId,
        CONCAT('placement_request:',pr.id) AS requestForPlacementId,
        CAST(pr.id as TEXT) AS matchRequestId,
        'STANDARD' AS requestForPlacementType, 
        pr.expected_arrival as requestedArrivalDate,
        pr.duration as requestedDurationDays,
        CAST(a.submitted_at as date) as requestForPlacementSubmittedAt,
        pr.withdrawal_reason as requestForPlacementWithdrawalReason,
        CAST(assess.submitted_at as date) as requestForPlacementAssessedDate,
        CAST(b.id as TEXT) as placementId,
        cr.name as placementCancellationReason
        FROM placement_requests pr
        INNER JOIN applications a ON pr.application_id = a.id
        INNER JOIN approved_premises_applications apa ON a.id = apa.id
        INNER JOIN assessments as assess ON assess.id = pr.assessment_id
        LEFT OUTER JOIN bookings as b ON b.id = pr.booking_id
        LEFT OUTER JOIN cancellations as c ON c.booking_id = b.id
        LEFT OUTER JOIN cancellation_reasons as cr ON c.cancellation_reason_id = cr.id
        WHERE
            pr.reallocated_at IS NULL AND
            pr.placement_application_id IS NULL AND
            date_part('month', pr.expected_arrival) = :month AND
            date_part('year', pr.expected_arrival) = :year AND
            a.service = 'approved-premises'
    """

    /**
     * Prior to March 2024, placement requests could have more than one associated
     * placement_application_dates entries. Unfortunately, we didn't maintain a
     * FK between these entries and their corresponding placement_requests.
     *
     * The inline query used in the FROM statement below links each placement_application_dates
     * to their corresponding placement_request by matching on placement_applications.id, start date
     * & duration.
     */
    private const val OTHER_REQUEST_FOR_PLACEMENT_QUERY = """
      SELECT 
      a.crn as crn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      CAST(a.id as TEXT) as applicationId,
      CONCAT('placement_application:',pa.id) AS requestForPlacementId,
      CAST(pr.id as TEXT) AS matchRequestId,
      CASE
        WHEN pa.placement_type = '0' THEN 'ROTL'
        WHEN pa.placement_type = '1' THEN 'RELEASE_FOLLOWING_DECISION'
        WHEN pa.placement_type = '2' THEN 'ADDITIONAL_PLACEMENT'
        ELSE ''
      END AS requestForPlacementType, 
      pr.expected_arrival as requestedArrivalDate,
      pr.duration as requestedDurationDays,
      CAST(pa.submitted_at as date) as requestForPlacementSubmittedAt,
      pa.withdrawal_reason as requestForPlacementWithdrawalReason,
      CAST(pa.decision_made_at as date) as requestForPlacementAssessedDate,
      CAST(b.id as TEXT) as placementId,
      cr.name as placementCancellationReason
      from
      (
	      SELECT
           pr.id as placement_request_id,
           pa.id as placement_application_id,
           pa_dates.id as placement_application_dates_id
			  FROM placement_requests pr
          INNER JOIN placement_applications pa ON pa.id = pr.placement_application_id
          INNER JOIN placement_application_dates pa_dates ON 
                    pa_dates.placement_application_id = pa.id AND 
                    pa_dates.expected_arrival = pr.expected_arrival AND
                    pa_dates.duration = pr.duration
			  WHERE pr.reallocated_at IS NULL
      ) as pr_to_dates
      inner join placement_requests pr on pr.id = pr_to_dates.placement_request_id 
      INNER JOIN placement_applications pa ON pa.id = pr_to_dates.placement_application_id
      INNER JOIN placement_application_dates pa_dates ON pa_dates.id = pr_to_dates.placement_application_dates_id
      INNER JOIN applications a ON pr.application_id = a.id
      INNER JOIN approved_premises_applications apa ON a.id = apa.id
      LEFT OUTER JOIN bookings as b ON b.id = pr.booking_id
      LEFT OUTER JOIN cancellations as c ON c.booking_id = b.id
      LEFT OUTER JOIN cancellation_reasons as cr ON c.cancellation_reason_id = cr.id
      WHERE
          pa.decision = 'ACCEPTED' AND
          date_part('month', pr.expected_arrival) = :month AND
          date_part('year', pr.expected_arrival) = :year AND
          a.service = 'approved-premises'
    """

    private const val QUERY = """ 
      $INITIAL_REQUEST_FOR_PLACEMENT_QUERY
      UNION ALL
      $OTHER_REQUEST_FOR_PLACEMENT_QUERY
      ORDER BY requestedArrivalDate ASC
    """
  }

  @Query(value = QUERY, nativeQuery = true)
  fun generateReportRowsForExpectedArrivalMonth(month: Int, year: Int): List<PlacementMatchingOutcomesEntityReportRow>
}

@SuppressWarnings("TooManyFunctions")
interface PlacementMatchingOutcomesEntityReportRow {
  fun getCrn(): String?
  fun getTier(): String?
  fun getApplicationId(): String?
  fun getRequestForPlacementId(): String?
  fun getMatchRequestId(): String?
  fun getRequestForPlacementType(): String?
  fun getRequestedArrivalDate(): Date?
  fun getRequestedDurationDays(): Int?
  fun getRequestForPlacementSubmittedAt(): Date?
  fun getRequestForPlacementWithdrawalReason(): String?
  fun getRequestForPlacementAssessedDate(): Date?
  fun getPlacementId(): String?
  fun getPlacementCancellationReason(): String?
}
