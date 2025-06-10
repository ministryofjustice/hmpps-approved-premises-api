package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ExcelJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate
import java.time.LocalDateTime

@Repository
class Cas1OutOfServiceBedsReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {
    const val QUERY = """
      WITH latest_revisions AS (
          SELECT DISTINCT ON (out_of_service_bed_id)
              out_of_service_bed_id,
              reference_number,
              start_date,
              end_date,
              cas1_out_of_service_bed_revisions.created_at AS max_created_at,
              revision_reason.name AS reason,
              revision_reason.id AS reason_id
          FROM cas1_out_of_service_bed_revisions
          LEFT JOIN cas1_out_of_service_bed_reasons AS revision_reason
              ON cas1_out_of_service_bed_revisions.out_of_service_bed_reason_id = revision_reason.id
          ORDER BY out_of_service_bed_id, cas1_out_of_service_bed_revisions.created_at DESC
      )
      SELECT
          rooms.name AS "roomName",
          beds.name AS "bedName",
          oos_bed.id AS id,
          latest_revisions.reference_number AS "workOrderId",
          probation_regions.name AS region,
          premises.name AS ap,
          latest_revisions.reason AS reason,
          latest_revisions.start_date AS "startDate",
          latest_revisions.end_date AS "endDate",
          CASE
            WHEN (
                DATE_PART('day', LEAST(:endDate, latest_revisions.end_date) - GREATEST(:startDate, latest_revisions.start_date)) + 1 > 0
            ) THEN (
                DATE_PART('day', LEAST(:endDate, latest_revisions.end_date) - GREATEST(:startDate, latest_revisions.start_date)) + 1
            )
            ELSE 0
          END AS "lengthDays",
          STRING_AGG(
              CONCAT(
                  'Date/Time: ', TO_CHAR(revision.created_at, 'FMDay FMDD FMMonth YYYY'),
                  E'\nReason: ', COALESCE(revision_reason.name, 'N/A'),
                  E'\nNotes: ', COALESCE(revision.notes, '')
              ),
              E'\n\n' ORDER BY revision.created_at
          ) AS notes
        FROM cas1_out_of_service_beds AS oos_bed
            JOIN beds ON oos_bed.bed_id = beds.id
            JOIN rooms ON beds.room_id = rooms.id
            JOIN premises ON rooms.premises_id = premises.id
            JOIN probation_regions ON premises.probation_region_id = probation_regions.id
            LEFT JOIN latest_revisions
                ON oos_bed.id = latest_revisions.out_of_service_bed_id
            LEFT JOIN cas1_out_of_service_bed_cancellations AS c
                ON oos_bed.id = c.out_of_service_bed_id
            LEFT JOIN cas1_out_of_service_bed_revisions AS revision
                ON revision.out_of_service_bed_id = oos_bed.id
            LEFT JOIN cas1_out_of_service_bed_reasons AS revision_reason
                ON revision.out_of_service_bed_reason_id = revision_reason.id
        WHERE
            latest_revisions.start_date <= :endDate
            AND latest_revisions.end_date >= :startDate
            AND c IS NULL
            AND latest_revisions.reason_id != :bedOnHoldReasonId
        GROUP BY
            oos_bed.id,
            rooms.name,
            beds.name,
            premises.name,
            probation_regions.name,
            latest_revisions.start_date,
            latest_revisions.end_date,
            latest_revisions.reference_number,
            latest_revisions.reason
        ORDER BY oos_bed.id
    """
  }

  fun generateOutOfServiceBedsReport(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    jdbcResultSetConsumer: ExcelJdbcResultSetConsumer,
  ) = reportJdbcTemplate.query(
    QUERY,
    mapOf<String, Any>(
      "startDate" to startDate,
      "endDate" to endDate,
      "bedOnHoldReasonId" to Cas1OutOfServiceBedReasonRepository.BED_ON_HOLD_REASON_ID,
    ),
    jdbcResultSetConsumer,
  )
}
