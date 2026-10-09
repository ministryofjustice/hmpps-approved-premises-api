package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.Cas1DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.reporting.CsvJdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DataScienceReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.scheduled.CasDataScienceReportScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAllCasDataScienceReportEntities
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.documentManagementApiMockSuccessfulUploadAny
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.documentManagementApiMockUnsuccessfulUploadAny
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class CasDataScienceReportScheduledJobIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var casDataScienceReportScheduledJob: CasDataScienceReportScheduledJob

  @Autowired
  lateinit var cas1Repo: Cas1DataScienceReportRepository

  @Autowired
  lateinit var cas2Repo: Cas2DataScienceReportRepository

  @Autowired
  lateinit var cas3Repo: Cas3DataScienceReportRepository

  @Autowired
  lateinit var dataSetup: DocumentUploadDataSetup

  @BeforeEach
  fun clearShedlockKeys() {
    redisTemplate.keys("*cas_data_science_reports_upload*").forEach(redisTemplate::delete)
  }

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
  fun `scheduled job generates and uploads CAS1, CAS2, and CAS3 data science reports to Document Management API`() {
    val createdEntities = givenAllCasDataScienceReportEntities()

    assertThat(createdEntities.cas1.application.id).isNotNull
    assertThat(createdEntities.cas2.application.id).isNotNull
    assertThat(createdEntities.cas3.application.id).isNotNull

    val sampleDocument = dataSetup.createSampleDocument()
    documentManagementApiMockSuccessfulUploadAny(
      documentType = "CAS_DOCUMENTS",
      response = sampleDocument,
    )

    val today = LocalDate.now()
    casDataScienceReportScheduledJob.generateAndUploadReports()

    listOf("CAS1", "CAS2", "CAS3").forEach { serviceType ->
      val expectedFilename = "${serviceType.lowercase()}_data_science_report_$today.csv"
      wiremockServer.verify(
        postRequestedFor(urlPathMatching("/documents/CAS_DOCUMENTS/.*"))
          .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
          .withAnyRequestBodyPart(
            aMultipart()
              .withName("file")
              .withHeader("Content-Disposition", containing("filename=\"$expectedFilename\""))
              .withHeader("Content-Type", containing("text/csv")),
          )
          .withAnyRequestBodyPart(
            aMultipart()
              .withName("metadata")
              .withHeader("Content-Type", containing("application/json"))
              .withBody(equalToJson("""{"date":"$today","documentSubType":"$serviceType"}""")),
          ),
      )
    }
  }

  @Test
  fun `scheduled job throws exception when Document Management API upload fails`() {
    givenAllCasDataScienceReportEntities()

    documentManagementApiMockUnsuccessfulUploadAny(
      documentType = "CAS_DOCUMENTS",
      responseStatus = 500,
    )

    assertThatThrownBy {
      casDataScienceReportScheduledJob.generateAndUploadReports()
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("CAS Data Science report scheduled execution completed with failures")
  }
}
