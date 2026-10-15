package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement.Document
import java.time.LocalDateTime
import java.util.UUID

@Component
class DocumentUploadDataSetup {

  fun createSampleDocument(
    documentUuid: UUID = UUID.randomUUID(),
    documentType: String = "CAS_DOCUMENTS",
    filename: String = "cas1_data_science_report_2026-10-06.csv",
    fileSize: Long = 1024L,
    mimeType: String = "text/csv",
  ): Document = Document(
    documentUuid = documentUuid,
    documentType = documentType,
    documentFilename = filename,
    filename = filename,
    fileExtension = "csv",
    fileSize = fileSize,
    fileHash = "sample-sha256-hash",
    mimeType = mimeType,
    createdTime = LocalDateTime.now(),
  )

  fun createSampleCsvContent(): ByteArray = "header1,header2,header3\nval1,val2,val3\nval4,val5,val6\n".toByteArray()
}
