package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import java.time.LocalDate

@Service
class CasDataScienceReportPublishingService(
  private val casDataScienceReportGenerator: CasDataScienceReportGenerator,
  private val documentManagementApiClient: DocumentManagementApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    const val CAS_DOCUMENT_TYPE = "CAS_DOCUMENTS"
  }

  /**
   * Generates and uploads all Data Science CSV reports for CAS1, CAS2, and CAS3.
   *
   * Each report generation and upload is executed within an isolated try-catch block.
   * If any report fails, processing continues to allow subsequent reports to run and collect full diagnostics.
   * If any failures occur, an aggregatedException [IllegalStateException] is thrown containing all failed services,
   * with individual causes attached as suppressed exceptions to ensure the overall job is marked as failed.
   */
  fun generateAndUploadAllReports(executionDate: LocalDate) {
    log.info("Starting CAS Data Science reports generation and upload for execution date: $executionDate")
    val failures = mutableListOf<DocumentUploadFailure>()

    CasDocumentSubType.entries.forEach { documentSubType ->
      try {
        generateAndUploadReport(documentSubType, executionDate)
      } catch (ex: Exception) {
        log.error("Failed to generate or upload ${documentSubType.value} report for date $executionDate", ex)
        failures.add(DocumentUploadFailure(documentSubType.value, ex))
      }
    }

    if (failures.isNotEmpty()) {
      val failedServices = failures.joinToString(", ") { it.service }
      val errorMessage = "CAS Data Science report scheduled execution completed with failures for services: $failedServices"
      log.error(errorMessage)
      val aggregatedException = IllegalStateException(errorMessage)
      failures.forEach { failure -> aggregatedException.addSuppressed(failure.cause) }
      throw aggregatedException
    }

    log.info("Successfully generated and uploaded all CAS Data Science reports for execution date: $executionDate")
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
  @SuppressWarnings("TooGenericExceptionCaught", "TooGenericExceptionThrown", "ThrowsCount")
  private fun generateAndUploadReport(documentSubType: CasDocumentSubType, executionDate: LocalDate) {
    val report = casDataScienceReportGenerator.generateReport(documentSubType, executionDate)
    val tempFile = report.tempFile

    try {
      /**
       * Metadata key-value pairs attached to the document payload.
       * Enables searching and filtering documents in the Document Management API (`POST /documents/search`)
       * by documentType (`CAS_DOCUMENTS`), report run date (`date`), and service subtype (`documentSubType`).
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
          val errorMsg = "Document Management API returned HTTP status ${uploadResult.status} for ${documentSubType.value} report upload: ${uploadResult.body}"
          log.error(errorMsg)
          throw RuntimeException(errorMsg)
        }
        is ClientResult.Failure.Other -> {
          val errorMsg = "Failed to communicate with Document Management API for ${documentSubType.value} report upload: ${uploadResult.exception.message}"
          log.error(errorMsg, uploadResult.exception)
          throw RuntimeException(errorMsg, uploadResult.exception)
        }
        else -> {
          val errorMsg = "Unexpected failure uploading ${documentSubType.value} report to Document Management API"
          log.error(errorMsg)
          throw RuntimeException(errorMsg)
        }
      }
    } finally {
      try {
        if (tempFile.exists()) {
          val deleted = tempFile.delete()
          if (!deleted) {
            log.warn("Failed to delete temporary report file: ${tempFile.absolutePath}")
          }
        }
      } catch (cleanupEx: Exception) {
        log.warn("Exception while deleting temporary report file: ${tempFile.absolutePath}", cleanupEx)
      }
    }
  }

  private data class DocumentUploadFailure(
    val service: String,
    val cause: Throwable,
  )
}
