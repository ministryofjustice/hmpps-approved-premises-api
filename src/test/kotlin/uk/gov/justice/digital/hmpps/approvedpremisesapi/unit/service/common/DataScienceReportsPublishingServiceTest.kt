package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.GeneratedReport
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.S3Config
import java.io.File
import java.time.LocalDate

class DataScienceReportsPublishingServiceTest {

  private val generator = mockk<DataScienceReportsGenerator>()
  private val s3Client = mockk<S3Client>()
  private val s3Config = S3Config().apply {
    bucketName = "test-bucket"
    region = "eu-west-2"
  }
  private val tempFiles = mutableListOf<File>()

  private val publishingService = DataScienceReportsPublishingService(
    dataScienceReportsGenerator = generator,
    s3Client = s3Client,
    s3Config = s3Config,
    tempFileFactory = { prefix, suffix ->
      File.createTempFile(prefix, suffix).also { tempFiles.add(it) }
    },
  )

  private val executionDate = LocalDate.of(2026, 10, 8)

  private fun givenGeneratedReport(documentSubType: CasDocumentSubType) {
    every { generator.generateReport(documentSubType, any()) } returns GeneratedReport(
      filename = "${documentSubType.value.lowercase()}_data_science_report_20261008_143000.csv",
      documentSubType = documentSubType,
    )
  }

  @Test
  fun `generateAndUploadReport successfully generates, uploads to S3 with executionDate prefix, and deletes temp file`() {
    givenGeneratedReport(CasDocumentSubType.CAS1)

    val requestSlot = slot<PutObjectRequest>()
    every { s3Client.putObject(capture(requestSlot), any<RequestBody>()) } returns PutObjectResponse.builder().build()

    publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)

    assertThat(requestSlot.captured.bucket()).isEqualTo("test-bucket")
    assertThat(requestSlot.captured.key()).isEqualTo("2026-10-08/cas1_data_science_report_20261008_143000.csv")
    assertThat(requestSlot.captured.contentType()).isEqualTo("text/csv")
    assertThat(tempFiles).isNotEmpty.allSatisfy { assertThat(it).doesNotExist() }

    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS1, any()) }
    verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
  }

  @Test
  fun `generateAndUploadReport does not retry at service level when upload fails, rethrows exception, and deletes temp file`() {
    givenGeneratedReport(CasDocumentSubType.CAS3)

    every {
      s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())
    } throws SdkClientException.create("S3 upload failed")

    assertThatThrownBy {
      publishingService.generateAndUploadReport(CasDocumentSubType.CAS3, executionDate)
    }.isInstanceOf(SdkClientException::class.java).hasMessage("S3 upload failed")

    assertThat(tempFiles).isNotEmpty.allSatisfy { assertThat(it).doesNotExist() }

    verify(exactly = 1) { generator.generateReport(CasDocumentSubType.CAS3, any()) }
    verify(exactly = 1) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
  }

  @Test
  fun `generateAndUploadReport cleans up temp file when generator throws exception`() {
    every { generator.generateReport(any(), any()) } throws RuntimeException("DB error during report generation")

    assertThatThrownBy {
      publishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)
    }.isInstanceOf(RuntimeException::class.java).hasMessage("DB error during report generation")

    assertThat(tempFiles).isNotEmpty.allSatisfy { assertThat(it).doesNotExist() }

    verify(exactly = 0) { s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) }
  }
}
