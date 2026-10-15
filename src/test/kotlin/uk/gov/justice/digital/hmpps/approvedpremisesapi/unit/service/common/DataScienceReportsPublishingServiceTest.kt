package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.GeneratedReport
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DataScienceReportsPublishingServiceTest {

  private val generator = mockk<DataScienceReportsGenerator>()
  private val apiClient = mockk<DocumentManagementApiClient>()

  private val publishingService = DataScienceReportsPublishingService(
    dataScienceReportsGenerator = generator,
    documentManagementApiClient = apiClient,
  )

  @Test
  fun `generateAndUploadAllReports successfully generates, uploads, and deletes temp files for all CAS services`() {
    val executionDate = LocalDate.of(2026, 10, 8)
    val tempFiles = mutableListOf<File>()

    val publishingService = DataScienceReportsPublishingService(
      dataScienceReportsGenerator = generator,
      documentManagementApiClient = apiClient,
      tempFileFactory = { prefix, suffix ->
        val file = File.createTempFile(prefix, suffix)
        tempFiles.add(file)
        file
      },
    )

    CasDocumentSubType.entries.forEach { subType ->
      val documentUuid = UUID.randomUUID()

      every { generator.generateReport(subType, executionDate, any()) } returns GeneratedReport(
        filename = "${subType.value.lowercase()}_data_science_report_$executionDate.csv",
        documentUuid = documentUuid,
        documentSubType = subType,
        executionDate = executionDate,
      )

      val document = Document(
        documentUuid = documentUuid,
        documentType = "CAS_DOCUMENTS",
        documentFilename = "${subType.value.lowercase()}_data_science_report_$executionDate.csv",
        filename = "${subType.value.lowercase()}_data_science_report_$executionDate.csv",
        fileExtension = "csv",
        fileSize = 100L,
        fileHash = "abc",
        mimeType = "text/csv",
        createdTime = LocalDateTime.now(),
      )

      every {
        apiClient.uploadDocument(
          documentType = "CAS_DOCUMENTS",
          documentUuid = documentUuid,
          filename = "${subType.value.lowercase()}_data_science_report_$executionDate.csv",
          fileResource = any(),
          metadata = mapOf("date" to executionDate.toString(), "documentSubType" to subType.value),
        )
      } returns ClientResult.Success(HttpStatus.CREATED, document, false)
    }

    publishingService.generateAndUploadAllReports(executionDate)

    tempFiles.forEach { tempFile ->
      assertThat(tempFile).doesNotExist()
    }

    verify(exactly = 3) { apiClient.uploadDocument(any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `generateAndUploadAllReports continues on failure, deletes temp file, and throws composite exception`() {
    val executionDate = LocalDate.of(2026, 10, 8)
    val tempFiles = mutableListOf<File>()

    val publishingService = DataScienceReportsPublishingService(
      dataScienceReportsGenerator = generator,
      documentManagementApiClient = apiClient,
      tempFileFactory = { prefix, suffix ->
        val file = File.createTempFile(prefix, suffix)
        tempFiles.add(file)
        file
      },
    )

    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()

    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1.csv",
      documentUuid = uuid1,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )
    every { generator.generateReport(CasDocumentSubType.CAS2, executionDate, any()) } returns GeneratedReport(
      filename = "cas2.csv",
      documentUuid = uuid2,
      documentSubType = CasDocumentSubType.CAS2,
      executionDate = executionDate,
    )
    every { generator.generateReport(CasDocumentSubType.CAS3, executionDate, any()) } returns GeneratedReport(
      filename = "cas3.csv",
      documentUuid = uuid3,
      documentSubType = CasDocumentSubType.CAS3,
      executionDate = executionDate,
    )

    // CAS1 succeeds
    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid1, "cas1.csv", any(), any(), any())
    } returns ClientResult.Success(HttpStatus.CREATED, mockk(relaxed = true), false)

    // CAS2 fails
    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid2, "cas2.csv", any(), any(), any())
    } returns ClientResult.Failure.StatusCode(HttpMethod.POST, "/documents/CAS_DOCUMENTS/$uuid2", HttpStatus.INTERNAL_SERVER_ERROR, "Server Error")

    // CAS3 succeeds
    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid3, "cas3.csv", any(), any(), any())
    } returns ClientResult.Success(HttpStatus.CREATED, mockk(relaxed = true), false)

    assertThatThrownBy {
      publishingService.generateAndUploadAllReports(executionDate)
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("failures for services: CAS2")

    // Ensure all temp files were deleted including the failed CAS2 file
    tempFiles.forEach { tempFile ->
      assertThat(tempFile).doesNotExist()
    }

    // Verify all 3 were attempted
    verify(exactly = 3) { generator.generateReport(any(), any(), any()) }
  }
}
