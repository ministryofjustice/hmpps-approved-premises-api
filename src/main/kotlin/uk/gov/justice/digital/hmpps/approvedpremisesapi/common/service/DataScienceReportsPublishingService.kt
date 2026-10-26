package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.S3Config
import java.io.File
import java.time.LocalDate

@Service
class DataScienceReportsPublishingService(
  private val dataScienceReportsGenerator: DataScienceReportsGenerator,
  private val s3Client: S3Client,
  private val s3Config: S3Config,
  private val tempFileFactory: (prefix: String, suffix: String) -> File = { prefix, suffix -> File.createTempFile(prefix, suffix) },
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Generates and uploads an individual CAS Data Science report to S3.
   *
   * The uploaded file is stored in S3 at the path: `<executionDate>/<filename>`
   * (e.g. `2026-10-24/cas1_data_science_report_2026-10-24.csv`).
   *
   * Transient upload failures are retried by the [S3Client] retry strategy (see `S3ClientConfig`).
   */
  fun generateAndUploadReport(documentSubType: CasDocumentSubType, executionDate: LocalDate) {
    val tempFile = tempFileFactory("cas_data_science_${documentSubType.value.lowercase()}", ".csv")

    try {
      val report = dataScienceReportsGenerator.generateReport(documentSubType, tempFile)

      uploadReportToS3(executionDate, report, tempFile)
    } finally {
      if (tempFile.exists()) {
        val deleted = tempFile.delete()
        if (!deleted) {
          log.warn("Failed to delete temporary report file: ${tempFile.absolutePath}")
        }
      }
    }
  }

  private fun uploadReportToS3(
    executionDate: LocalDate,
    report: GeneratedReport,
    tempFile: File,
  ) {
    val key = "$executionDate/${report.filename}"
    val request = PutObjectRequest.builder()
      .bucket(s3Config.bucketName)
      .key(key)
      .contentType("text/csv")
      .build()

    s3Client.putObject(request, RequestBody.fromFile(tempFile))
    log.info("Successfully uploaded ${report.documentSubType.value} report to S3 bucket ${s3Config.bucketName} with key $key")
  }
}
