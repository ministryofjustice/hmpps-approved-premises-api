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
  /*
  TODO:

  Only show pending or compete - do not show withdrawn. Includes all data to the right

  matchRequestId - the placement_request id
  matcherCru - allocated ap area
  matcherUsername - allocated user name
  matchOutcome - 'placed' or 'not matched'
   */

  fun buildQuery(): String {
    val cte = cas1RequestForPlacementReportRepository.buildQuery(
      placementRequestsRangeConstraints = """
        (pr.expected_arrival >= :startDateTimeInclusive AND pr.expected_arrival <= :endDateTimeInclusive)
      """.trimIndent(),
      placementApplicationsRangeConstraints = """
        (pa_date.expected_arrival >= :startDateTimeInclusive AND pa_date.expected_arrival <= :endDateTimeInclusive)
      """.trimIndent()
    )

    return """
      WITH raw_requests_for_placements AS ($cte)
      SELECT 
        pr.id as match_request_id,
        '' as matcher_cru,
        '' as matcher_username,
        '' as match_outcome,
        rfp.* 
      FROM raw_requests_for_placements rfp
      INNER JOIN placement_requests pr ON pr.id = rfp.internal_placement_request_id
      LEFT OUTER JOIN (
         select distinct on (application_id)
                 domain_events.*
         from domain_events
         where reallocated_at is null
         order by application_id, created_at desc
      ) latest_match_outcome_event on latest_assessment.application_id = a.id
      WHERE rfp.internal_placement_request_id IS NOT NULL
      ORDER BY pr.expected_arrival ASC
    """.trimIndent()
  }

  // link on latest domain event of type 'APPROVED_PREMISES_BOOKING_MADE' or 'APPROVED_PREMISES_BOOKING_NOT_MADE'. this drives outcome

  val query = buildQuery()

  fun generateForArrivalDateThisMonth(
    startDateTimeInclusive: LocalDateTime,
    endDateTimeInclusive: LocalDateTime,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) =
    reportJdbcTemplate.query(
      query,
      mapOf<String, Any>(
        "startDateTimeInclusive" to startDateTimeInclusive,
        "endDateTimeInclusive" to endDateTimeInclusive,
      ),
      jbdcResultSetConsumer,
    )
}
