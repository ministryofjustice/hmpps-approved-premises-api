package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.UUID

@Repository
interface ApplicationTimelinessEntityRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query(
    """
    SELECT
      CAST(application.id as TEXT) as id, 
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      submission_event.occurred_at as applicationSubmittedAt,
      booking_made_event.occurred_at as bookingMadeAt,
      DATE_PART(
        'day',
        booking_made_event.occurred_at - submission_event.occurred_at
      ) as overallTimeliness,
      DATE_PART(
        'day',
        booking_made_event.occurred_at - assesment_event.occurred_at
      ) as placementMatchingTimeliness
    from
      applications application
      left join approved_premises_applications apa on application.id = apa.id
      left join domain_events submission_event on submission_event.type = 'APPROVED_PREMISES_APPLICATION_SUBMITTED'
      and application.id = submission_event.application_id
      left join domain_events assesment_event on assesment_event.type = 'APPROVED_PREMISES_APPLICATION_ASSESSED'
      and application.id = assesment_event.application_id
      left join domain_events booking_made_event on booking_made_event.type = 'APPROVED_PREMISES_BOOKING_MADE'
      and application.id = booking_made_event.application_id
    where 
      date_part('month', application.submitted_at) = :month
      AND date_part('year', application.submitted_at) = :year
  """,
    nativeQuery = true,
  )
  fun findAllForMonthAndYear(month: Int, year: Int): List<ApplicationTimelinessEntity>
}

interface ApplicationTimelinessEntity {
  fun getId(): String
  fun getTier(): String?
  fun getApplicationSubmittedAt(): Timestamp?
  fun getBookingMadeAt(): Timestamp?
  fun getOverallTimeliness(): Int?
  fun getPlacementMatchingTimeliness(): Int?
}
