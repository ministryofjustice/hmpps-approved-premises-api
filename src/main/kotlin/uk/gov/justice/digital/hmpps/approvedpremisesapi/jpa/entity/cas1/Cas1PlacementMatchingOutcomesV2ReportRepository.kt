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
      raw_requests_for_placements.* 
      FROM raw_requests_for_placements;
    """.trimIndent()
  }

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
