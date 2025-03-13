package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1PlacementMatchingOutcomesV2ReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
  val cas1RequestForPlacementReportRepository: Cas1RequestForPlacementReportRepository,
) {

  companion object {
    const val CORE_QUERY = """
      SELECT 
        pr.id as placement_request_id,
        
        CASE
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_MADE' THEN latest_match_attempt_event.data -> 'eventDetails' -> 'bookedBy' -> 'cru' ->> 'name'
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_NOT_MADE' THEN latest_match_attempt_event.data -> 'eventDetails' -> 'attemptedBy' -> 'cru' ->> 'name'
          ELSE ''
        END as matcher_cru,
        CASE
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_MADE' THEN latest_match_attempt_event.data -> 'eventDetails' -> 'bookedBy' -> 'staffMember' ->> 'username'
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_NOT_MADE' THEN latest_match_attempt_event.data -> 'eventDetails' -> 'attemptedBy' -> 'staffMember' ->> 'username'
          ELSE ''
        END as matcher_username,
        CASE
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_MADE' THEN 'Placed'
          WHEN latest_match_attempt_event.type = 'APPROVED_PREMISES_BOOKING_NOT_MADE' THEN 'Not matched'
          ELSE ''
        END as match_outcome,
        
        to_char(first_match_attempt_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as first_match_attempt_date_time,
        to_char(first_match_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as first_successful_match_attempt_date_time,
        first_match_event.data -> 'eventDetails' -> 'premises' ->> 'name' as first_successful_match_attempt_premises_name,
        to_char(latest_match_event.occurred_at, 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as last_successful_match_attempt_date_time,
        latest_match_event.data -> 'eventDetails' -> 'premises' ->> 'name' as last_successful_match_attempt_premises_name,
        
        rfp.* 
      FROM raw_requests_for_placements rfp
      
      LEFT OUTER JOIN placement_application_dates pad ON pad.id = rfp.internal_placement_application_date_id
      
      -- this is required because placement applications can have many dates linked to different placement requests
      INNER JOIN placement_requests pr ON (
        (
          rfp.internal_placement_request_id IS NOT NULL AND
          pr.id = rfp.internal_placement_request_id
        ) 
        OR
        (
          rfp.internal_placement_application_date_id IS NOT NULL AND 
          pr.id = pad.placement_request_id
        )
      )
      
      LEFT OUTER JOIN LATERAL (
         SELECT  evt.id,
                 evt.type,
                 evt.data,
                 evt.application_id,
                 evt.occurred_at,
                 m.value as placement_request_id
         FROM domain_events evt
         INNER JOIN domain_events_metadata m ON m.domain_event_id = evt.id AND m.name = 'CAS1_PLACEMENT_REQUEST_ID'
         WHERE type IN ('APPROVED_PREMISES_BOOKING_MADE','APPROVED_PREMISES_BOOKING_NOT_MADE') AND
         evt.application_id = pr.application_id AND 
         m.value = CAST(pr.id as TEXT)
         ORDER BY evt.application_id, m.value, created_at ASC
         LIMIT 1
      ) first_match_attempt_event ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery
      
      LEFT OUTER JOIN LATERAL (
         SELECT  evt.id,
                 evt.type,
                 evt.data,
                 evt.application_id,
                 evt.occurred_at,
                 m.value as placement_request_id
         FROM domain_events evt
         INNER JOIN domain_events_metadata m ON m.domain_event_id = evt.id AND m.name = 'CAS1_PLACEMENT_REQUEST_ID'
         WHERE evt.type IN ('APPROVED_PREMISES_BOOKING_MADE') AND
         evt.application_id = pr.application_id AND 
         m.value = CAST(pr.id as TEXT)
         ORDER BY evt.application_id, m.value, created_at ASC
         LIMIT 1
      ) first_match_event ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery
      
      LEFT OUTER JOIN LATERAL (
         SELECT  evt.id,
	             evt.type,
	             evt.data,
	             evt.application_id,
	             evt.occurred_at,
	             m.value as placement_request_id
         FROM domain_events evt
         INNER JOIN domain_events_metadata m ON m.domain_event_id = evt.id AND m.name = 'CAS1_PLACEMENT_REQUEST_ID'
         WHERE evt.type IN ('APPROVED_PREMISES_BOOKING_MADE','APPROVED_PREMISES_BOOKING_NOT_MADE') AND
         evt.application_id = pr.application_id AND 
         m.value = CAST(pr.id as TEXT)
         ORDER BY evt.application_id, m.value, created_at desc
         LIMIT 1
      ) latest_match_attempt_event ON TRUE -- ON condition is mandatory with LEFT OUTER JOIN, but satisfied already in lateral join subquery
                
      LEFT OUTER JOIN LATERAL (
         SELECT evt.id,
                evt.type,
                evt.data,
                evt.application_id,
                evt.occurred_at,
                m.value as placement_request_id
         FROM domain_events evt
         INNER JOIN domain_events_metadata m ON m.domain_event_id = evt.id AND m.name = 'CAS1_PLACEMENT_REQUEST_ID'
         WHERE evt.type IN ('APPROVED_PREMISES_BOOKING_MADE') AND
         evt.application_id = pr.application_id AND
         m.value = CAST(pr.id as TEXT)
         ORDER BY evt.application_id, m.value, created_at desc
         LIMIT 1
      ) latest_match_event on true -- ON condition is mandatory with LEFT OUTER JOIN, but lateral join already does all required 'joining' in the subquery
    
    """
  }

  fun buildQueryForPlacementReport(): String {
    val cte = cas1RequestForPlacementReportRepository.buildQuery(
      placementRequestsRangeConstraints = "true",
      placementApplicationsRangeConstraints = "true",
    )

    return """
      WITH raw_requests_for_placements AS ($cte)
      $CORE_QUERY
    """.trimIndent()
  }

  private final fun buildQueryWithRange(): String {
    val cte = cas1RequestForPlacementReportRepository.buildQuery(
      placementRequestsRangeConstraints = """
        (pr.expected_arrival >= :startDateTimeInclusive AND pr.expected_arrival <= :endDateTimeInclusive)
      """.trimIndent(),
      placementApplicationsRangeConstraints = """
        (pa_date.expected_arrival >= :startDateTimeInclusive AND pa_date.expected_arrival <= :endDateTimeInclusive)
      """.trimIndent(),
    )

    return """
      WITH raw_requests_for_placements AS ($cte)
      $CORE_QUERY
      ORDER BY pr.expected_arrival ASC
    """.trimIndent()
  }

  val query = buildQueryWithRange()

  fun generateForArrivalDateThisMonth(
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
