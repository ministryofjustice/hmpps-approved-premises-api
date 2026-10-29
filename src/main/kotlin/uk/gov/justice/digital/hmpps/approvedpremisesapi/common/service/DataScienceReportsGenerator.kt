package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.Cas2DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.Cas3DataScienceReportRepository
import java.io.File
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DataScienceReportsGenerator(
  private val cas1DataScienceReportRepository: Cas1DataScienceReportRepository,
  private val cas2DataScienceReportRepository: Cas2DataScienceReportRepository,
  private val cas3DataScienceReportRepository: Cas3DataScienceReportRepository,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object {
    val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
  }

  @Transactional(readOnly = true)
  fun generateReport(documentSubType: CasDocumentSubType, targetFile: File): GeneratedReport {
    val timestamp = LocalDateTime.now(clock).format(TIMESTAMP_FORMAT)
    val filename = "${documentSubType.value.lowercase()}_data_science_report_$timestamp.csv"

    log.info("Generating $documentSubType Data Science report (filename: $filename)")

    targetFile.outputStream().buffered().use { outputStream ->
      CsvJdbcResultSetConsumer(outputStream).use { consumer ->
        when (documentSubType) {
          CasDocumentSubType.CAS1 -> cas1DataScienceReportRepository.generate(consumer)
          CasDocumentSubType.CAS2 -> cas2DataScienceReportRepository.generate(consumer)
          CasDocumentSubType.CAS3 -> cas3DataScienceReportRepository.generate(consumer)
        }
      }
    }

    val fileSizeBytes = targetFile.length()
    log.info("Generated $documentSubType CSV report (size: $fileSizeBytes bytes)")

    return GeneratedReport(
      filename = filename,
      documentSubType = documentSubType,
    )
  }
}

enum class CasDocumentSubType(val value: String) {
  CAS1("CAS1"),
  CAS2("CAS2"),
  CAS3("CAS3"),
}

data class GeneratedReport(
  val filename: String,
  val documentSubType: CasDocumentSubType,
)
