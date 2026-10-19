package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import java.io.File
import java.time.LocalDate

@Service
@SuppressWarnings("MagicNumber")
class DataScienceReportsPublishingService(
  private val dataScienceReportsGenerator: DataScienceReportsGenerator,
  private val documentManagementApiClient: DocumentManagementApiClient,
  private val tempFileFactory: (prefix: String, suffix: String) -> File = { prefix, suffix -> File.createTempFile(prefix, suffix) },
  private val retryTemplate: RetryTemplate = RetryTemplate.builder()
    .maxAttempts(3)
    .exponentialBackoff(5000, 2.0, 30000)
    .notRetryOn(listOf(IllegalArgumentException::class.java))
    .build(),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val CAS_DOCUMENT_TYPE = "CAS_DOCUMENTS"
    val RETRY_ERROR_CODES = listOf(500, 501, 502, 503, 504)
  }

  /**
   * Generates and uploads an individual CAS Data Science report.
   *
   * Document Management API Search & Metadata Usage:
   * The uploaded document is registered under documentType [CAS_DOCUMENT_TYPE] (`CAS_DOCUMENTS`)
   * and tagged with the following metadata attributes:
   * - `date`: The execution date (`YYYY-MM-DD`) on which the report was generated.
   * - `documentSubType`: The specific CAS service subtype (`CAS1`, `CAS2`, or `CAS3`).
   *
   * Downstream consumers can filter and search for these reports
   * via the Document Management API search endpoint (`POST /documents/search`) by specifying:
   * ```json
   * {
   *   "documentTypes": ["CAS_DOCUMENTS"],
   *   "metadata": {
   *     "date": "2026-10-09",
   *     "documentSubType": "CAS1"
   *   }
   * }
   * ```
   * OR (for all documents of a specific date, regardless of subtype):
   * ```json
   * {
   *   "documentTypes": ["CAS_DOCUMENTS"],
   *   "metadata": {
   *     "date": "2026-10-09"
   *   }
   * }
   * ```
   */
  fun generateAndUploadReport(documentSubType: CasDocumentSubType, executionDate: LocalDate) {
    val tempFile = tempFileFactory("cas_data_science_${documentSubType.value.lowercase()}", ".csv")

    try {
      val report = dataScienceReportsGenerator.generateReport(documentSubType, executionDate, tempFile)

      retryTemplate.execute<Unit, Throwable> {
        uploadDocumentReport(executionDate, documentSubType, report, tempFile)
      }
    } finally {
      if (tempFile.exists()) {
        val deleted = tempFile.delete()
        if (!deleted) {
          log.warn("Failed to delete temporary report file: ${tempFile.absolutePath}")
        }
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught", "TooGenericExceptionThrown", "ThrowsCount")
  private fun uploadDocumentReport(
    executionDate: LocalDate,
    documentSubType: CasDocumentSubType,
    report: GeneratedReport,
    tempFile: File,
  ) {
    /**
     * Metadata key-value pairs attached to the document payload.
     * Enables searching and filtering documents in the Document Management API (`POST /documents/search`)
     * by documentType (`CAS_DOCUMENTS`), report run date (`date`), and document subtype (`documentSubType`).
     */
    val metadata = mapOf(
      "date" to executionDate.toString(),
      "documentSubType" to documentSubType.value,
    )

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = CAS_DOCUMENT_TYPE,
      documentUuid = report.documentUuid,
      filename = report.filename,
      fileResource = FileSystemResource(tempFile),
      metadata = metadata,
    )

    when (uploadResult) {
      is ClientResult.Success -> {
        log.info("Successfully uploaded ${documentSubType.value} report to Document Management API (UUID: ${uploadResult.body.documentUuid}, S3 filename: ${uploadResult.body.filename})")
      }

      is ClientResult.Failure.StatusCode -> {
        val statusCode = uploadResult.status.value()
        val errorMsg =
          "Document Management API returned HTTP status $statusCode for ${documentSubType.value} report upload: ${uploadResult.body}"
        log.error(errorMsg)
        if (statusCode in RETRY_ERROR_CODES) {
          throw IllegalStateException(errorMsg)
        } else {
          throw IllegalArgumentException(errorMsg)
        }
      }

      is ClientResult.Failure.Other -> {
        val errorMsg =
          "Failed to communicate with Document Management API for ${documentSubType.value} report upload: ${uploadResult.exception.message}"
        log.error(errorMsg, uploadResult.exception)
        throw IllegalStateException(errorMsg, uploadResult.exception)
      }

      else -> {
        val errorMsg = "Unexpected failure uploading ${documentSubType.value} report to Document Management API"
        log.error(errorMsg)
        throw IllegalArgumentException(errorMsg)
      }
    }
  }
}
