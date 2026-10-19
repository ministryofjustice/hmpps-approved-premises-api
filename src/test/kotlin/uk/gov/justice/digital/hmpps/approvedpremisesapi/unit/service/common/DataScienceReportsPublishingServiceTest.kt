package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.retry.support.RetryTemplate
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

  private fun createFastRetryTemplate(): RetryTemplate = RetryTemplate.builder()
    .maxAttempts(3)
    .fixedBackoff(1)
    .notRetryOn(listOf(IllegalArgumentException::class.java))
    .build()

  @Test
  fun `generateAndUploadReport successfully generates, uploads, and deletes temp file`() {
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
      retryTemplate = createFastRetryTemplate(),
    )

    val documentUuid = UUID.randomUUID()
    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1_data_science_report_$executionDate.csv",
      documentUuid = documentUuid,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )

    val document = Document(
      documentUuid = documentUuid,
      documentType = "CAS_DOCUMENTS",
      documentFilename = "cas1_data_science_report_$executionDate.csv",
      filename = "cas1_data_science_report_$executionDate.csv",
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
        filename = "cas1_data_science_report_$executionDate.csv",
        fileResource = any(),
        metadata = mapOf("date" to executionDate.toString(), "documentSubType" to "CAS1"),
      )
    } returns ClientResult.Success(HttpStatus.CREATED, document, false)

    publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)

    tempFiles.forEach { tempFile ->
      assertThat(tempFile).doesNotExist()
    }

    verify(exactly = 1) { generator.generateReport(any(), any(), any()) }
    verify(exactly = 1) { apiClient.uploadDocument(any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `generateAndUploadReport generates once and retries upload on transient failure then succeeds on next attempt`() {
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
      retryTemplate = createFastRetryTemplate(),
    )

    val uuid = UUID.randomUUID()
    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1.csv",
      documentUuid = uuid,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )

    val document = Document(
      documentUuid = uuid,
      documentType = "CAS_DOCUMENTS",
      documentFilename = "cas1.csv",
      filename = "cas1.csv",
      fileExtension = "csv",
      fileSize = 100L,
      fileHash = "abc",
      mimeType = "text/csv",
      createdTime = LocalDateTime.now(),
    )

    // Attempt 1: 503 Service Unavailable, Attempt 2: Success
    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any())
    } returns ClientResult.Failure.StatusCode(
      HttpMethod.POST,
      "/documents/CAS_DOCUMENTS/$uuid",
      HttpStatus.SERVICE_UNAVAILABLE,
      "Unavailable",
    ) andThen ClientResult.Success(HttpStatus.CREATED, document, false)

    publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)

    // Report generation ran only ONCE
    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) }

    // Upload was attempted TWICE with the exact same UUID
    verify(exactly = 2) { apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any()) }

    tempFiles.forEach { assertThat(it).doesNotExist() }
  }

  @Test
  fun `generateAndUploadReport retries upload 3 times on 5xx server errors then exhausts and cleans up temp file`() {
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
      retryTemplate = createFastRetryTemplate(),
    )

    val uuid = UUID.randomUUID()
    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1.csv",
      documentUuid = uuid,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )

    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any())
    } returns ClientResult.Failure.StatusCode(HttpMethod.POST, "/documents/CAS_DOCUMENTS/$uuid", HttpStatus.BAD_GATEWAY, "Bad Gateway")

    assertThatThrownBy {
      publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("HTTP status 502")

    // Report generation ran only ONCE
    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) }

    // Upload was attempted 3 times (exhausted attempts)
    verify(exactly = 3) { apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any()) }

    tempFiles.forEach { assertThat(it).doesNotExist() }
  }

  @Test
  fun `generateAndUploadReport does not retry for 4xx client errors and fails on first attempt`() {
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
      retryTemplate = createFastRetryTemplate(),
    )

    val uuid = UUID.randomUUID()
    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1.csv",
      documentUuid = uuid,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )

    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any())
    } returns ClientResult.Failure.StatusCode(HttpMethod.POST, "/documents/CAS_DOCUMENTS/$uuid", HttpStatus.BAD_REQUEST, "Bad Request")

    assertThatThrownBy {
      publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("HTTP status 400")

    // Report generation ran only ONCE
    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) }

    // Upload was attempted only ONCE (no retry on 4xx)
    verify(exactly = 1) { apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any()) }

    tempFiles.forEach { assertThat(it).doesNotExist() }
  }

  @Test
  fun `generateAndUploadReport retries upload 3 times on network or communication failure then exhausts`() {
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
      retryTemplate = createFastRetryTemplate(),
    )

    val uuid = UUID.randomUUID()
    every { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) } returns GeneratedReport(
      filename = "cas1.csv",
      documentUuid = uuid,
      documentSubType = CasDocumentSubType.CAS1,
      executionDate = executionDate,
    )

    every {
      apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any())
    } returns ClientResult.Failure.Other(HttpMethod.POST, "/documents/CAS_DOCUMENTS/$uuid", RuntimeException("Connection timed out"))

    assertThatThrownBy {
      publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("Failed to communicate with Document Management API")

    // Report generation ran only ONCE
    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS1, executionDate, any()) }

    // Upload was attempted 3 times
    verify(exactly = 3) { apiClient.uploadDocument("CAS_DOCUMENTS", uuid, "cas1.csv", any(), any(), any()) }

    tempFiles.forEach { assertThat(it).doesNotExist() }
  }
}
