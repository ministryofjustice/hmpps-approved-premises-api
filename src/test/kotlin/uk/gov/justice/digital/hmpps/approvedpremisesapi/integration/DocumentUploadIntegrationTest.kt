package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.Cas2DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.Cas3DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDocumentSubType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.DataScienceReportsPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SchedulingConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAllCasDataScienceReportEntities
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate

class DocumentUploadIntegrationTest : IntegrationTestBase() {

  @MockkBean
  lateinit var s3Client: S3Client

  @Autowired
  lateinit var cas1Repo: Cas1DataScienceReportRepository

  @Autowired
  lateinit var cas2Repo: Cas2DataScienceReportRepository

  @Autowired
  lateinit var cas3Repo: Cas3DataScienceReportRepository

  @Autowired
  lateinit var dataScienceReportsPublishingService: DataScienceReportsPublishingService

  @Test
  fun `data science queries for CAS1, CAS2, and CAS3 produce CSV containing matching applications`() {
    val createdEntities = givenAllCasDataScienceReportEntities()

    val out1 = ByteArrayOutputStream()
    CsvJdbcResultSetConsumer(out1).use { cas1Repo.generate(it) }
    assertThat(out1.toString()).contains(createdEntities.cas1.application.crn)

    val out2 = ByteArrayOutputStream()
    CsvJdbcResultSetConsumer(out2).use { cas2Repo.generate(it) }
    assertThat(out2.toString()).contains(createdEntities.cas2.application.crn)

    val out3 = ByteArrayOutputStream()
    CsvJdbcResultSetConsumer(out3).use { cas3Repo.generate(it) }
    assertThat(out3.toString()).contains(createdEntities.cas3.application.crn)
  }

  @Test
  fun `generateAndUploadReport uploads CSV files to S3 for all CAS services under executionDate directory`() {
    val executionDate = LocalDate.of(2026, 10, 7)
    val capturedRequests = mutableListOf<PutObjectRequest>()

    every {
      s3Client.putObject(capture(capturedRequests), any<RequestBody>())
    } returns PutObjectResponse.builder().build()

    listOf(CasDocumentSubType.CAS1, CasDocumentSubType.CAS2, CasDocumentSubType.CAS3).forEach { subType ->
      dataScienceReportsPublishingService.generateAndUploadReport(subType, executionDate)
    }

    assertThat(capturedRequests).hasSize(3)
    assertThat(capturedRequests.map { it.key() }).anyMatch { it.matches(Regex("2026-10-07/cas1_data_science_report_\\d{8}_\\d{6}\\.csv")) }
    assertThat(capturedRequests.map { it.key() }).anyMatch { it.matches(Regex("2026-10-07/cas2_data_science_report_\\d{8}_\\d{6}\\.csv")) }
    assertThat(capturedRequests.map { it.key() }).anyMatch { it.matches(Regex("2026-10-07/cas3_data_science_report_\\d{8}_\\d{6}\\.csv")) }
    assertThat(capturedRequests.map { it.bucket() }).allMatch { it == "test-bucket" }
    assertThat(capturedRequests.map { it.contentType() }).allMatch { it == "text/csv" }
  }

  @Test
  fun `generateAndUploadReport uploads CSV report with generated application data to S3`() {
    val createdEntities = givenAllCasDataScienceReportEntities()
    val executionDate = LocalDate.of(2026, 10, 8)
    var capturedKey = ""
    var capturedBucket = ""
    var capturedContent = ""

    every {
      s3Client.putObject(any<PutObjectRequest>(), any<RequestBody>())
    } answers {
      val req = firstArg<PutObjectRequest>()
      val body = secondArg<RequestBody>()
      capturedKey = req.key()
      capturedBucket = req.bucket()
      val bytes = body.contentStreamProvider().newStream().readAllBytes()
      capturedContent = String(bytes)
      PutObjectResponse.builder().build()
    }

    dataScienceReportsPublishingService.generateAndUploadReport(CasDocumentSubType.CAS1, executionDate)

    assertThat(capturedKey).matches("2026-10-08/cas1_data_science_report_\\d{8}_\\d{6}\\.csv")
    assertThat(capturedBucket).isEqualTo("test-bucket")
    assertThat(capturedContent).contains(createdEntities.cas1.application.crn)
  }

  @Test
  fun `scheduled task error handler captures exception in Sentry when an unhandled error occurs`() {
    val scheduler = ThreadPoolTaskScheduler()
    SchedulingConfig().customiseTaskScheduler(mockSentryService).customize(scheduler)
    scheduler.initialize()

    val expectedException = RuntimeException("Scheduled job failed")

    try {
      scheduler.schedule(
        { throw expectedException },
        Instant.now(),
      )

      await().untilAsserted {
        assertThat(mockSentryService.getRaisedExceptions()).contains(expectedException)
      }
    } finally {
      scheduler.shutdown()
    }
  }
}
