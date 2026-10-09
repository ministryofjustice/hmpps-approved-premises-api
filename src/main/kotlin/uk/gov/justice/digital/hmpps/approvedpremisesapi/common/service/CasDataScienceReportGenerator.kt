package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DataScienceReportRepository
import java.io.File
import java.time.LocalDate
import java.util.UUID

@Service
class CasDataScienceReportGenerator(
  private val cas1DataScienceReportRepository: Cas1DataScienceReportRepository,
  private val cas2DataScienceReportRepository: Cas2DataScienceReportRepository,
  private val cas3DataScienceReportRepository: Cas3DataScienceReportRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional(readOnly = true)
  @SuppressWarnings("TooGenericExceptionCaught")
  fun generateReport(documentSubType: CasDocumentSubType, executionDate: LocalDate): GeneratedReport {
    val filename = "${documentSubType.value.lowercase()}_data_science_report_$executionDate.csv"
    val documentUuid = UUID.randomUUID()

    log.info("Generating $documentSubType Data Science report (filename: $filename, documentUuid: $documentUuid)")

    val tempFile = File.createTempFile("cas_data_science_${documentSubType.value.lowercase()}", ".csv")
    try {
      tempFile.outputStream().buffered().use { outputStream ->
        CsvJdbcResultSetConsumer(outputStream).use { consumer ->
          when (documentSubType) {
            CasDocumentSubType.CAS1 -> cas1DataScienceReportRepository.generate(consumer)
            CasDocumentSubType.CAS2 -> cas2DataScienceReportRepository.generate(consumer)
            CasDocumentSubType.CAS3 -> cas3DataScienceReportRepository.generate(consumer)
          }
        }
      }

      val fileSizeBytes = tempFile.length()
      log.info("Generated $documentSubType CSV report (size: $fileSizeBytes bytes)")

      return GeneratedReport(
        filename = filename,
        documentUuid = documentUuid,
        documentSubType = documentSubType,
        executionDate = executionDate,
        tempFile = tempFile,
      )
    } catch (ex: Exception) {
      if (tempFile.exists()) {
        try {
          val deleted = tempFile.delete()
          if (!deleted) {
            log.warn("Failed to delete temporary report file on generation failure: ${tempFile.absolutePath}")
          }
        } catch (cleanupEx: Exception) {
          log.warn("Exception while deleting temporary report file on generation failure: ${tempFile.absolutePath}", cleanupEx)
        }
      }
      throw ex
    }
  }
}

enum class CasDocumentSubType(val value: String) {
  CAS1("CAS1"),
  CAS2("CAS2"),
  CAS3("CAS3"),
}

data class GeneratedReport(
  val filename: String,
  val documentUuid: UUID,
  val documentSubType: CasDocumentSubType,
  val executionDate: LocalDate,
  val tempFile: File,
)
