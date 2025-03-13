package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import java.util.UUID

@Component
class Cas1UpdateAssessmentReportPropertiesJob(
  private val migrationLogger: MigrationLogger,
  private val updateAssessmentReportPropertiesRepository: UpdateAssessmentReportPropertiesRepository,
  override val shouldRunInTransaction: Boolean = true,
) : MigrationJob() {

  override fun process(pageSize: Int) = executeUpdateAssessmentReportProperties()

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun executeUpdateAssessmentReportProperties() {
    try {
      updateAssessmentReportPropertiesRepository.updateAssessmentReportProperties()
    } catch (exception: Exception) {
      migrationLogger.error("Unable to run update assessment report properties job", exception)
    }
  }
}

@Repository
interface UpdateAssessmentReportPropertiesRepository : JpaRepository<ApprovedPremisesAssessmentEntity, UUID> {

  companion object {
    private const val QUERY_UPDATE_ASSESSMENT_REPORT_PROPERTIES = """
    WITH assessment_data AS (
      SELECT
          a.id AS id,
          a.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReason' AS agreeWithShortNoticeReason,
          a.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'agreeWithShortNoticeReasonComments' AS agreeWithShortNoticeReasonComments,
          a.data -> 'suitability-assessment' -> 'application-timeliness' ->> 'reasonForLateApplication' AS reasonForLateApplication
      FROM
          assessments AS a
              INNER JOIN
          approved_premises_assessments AS apa
          ON
              apa.assessment_id = a.id
      WHERE
          a.submitted_at IS NOT NULL
    )
    UPDATE approved_premises_assessments AS target
      SET
          agree_with_short_notice_reason = CASE
                                               WHEN source.agreeWithShortNoticeReason = 'yes' THEN TRUE
                                               WHEN source.agreeWithShortNoticeReason = 'no' THEN FALSE
              END,
          agree_with_short_notice_reason_comments = NULLIF(source.agreeWithShortNoticeReasonComments, ''),
          reason_for_late_application = NULLIF(source.reasonForLateApplication, '')
      FROM assessment_data AS source
      WHERE target.assessment_id = source.id;
    """
  }

  @Query(QUERY_UPDATE_ASSESSMENT_REPORT_PROPERTIES, nativeQuery = true)
  @Modifying
  fun updateAssessmentReportProperties()
}
