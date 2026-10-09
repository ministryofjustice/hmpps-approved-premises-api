package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.http.Fault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CasDataScienceReportPublishingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.documentManagementApiMockSuccessfulUpload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.documentManagementApiMockSuccessfulUploadAny
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.documentManagementApiMockUnsuccessfulUpload
import java.time.LocalDate
import java.util.UUID

class DocumentUploadIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var documentManagementApiClient: DocumentManagementApiClient

  @Autowired
  lateinit var casDataScienceReportPublishingService: CasDataScienceReportPublishingService

  @Autowired
  lateinit var dataSetup: DocumentUploadDataSetup

  @Test
  fun `uploadDocument sends multipart file and metadata successfully`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val filename = "cas1_data_science_report_2026-10-06.csv"
    val csvContent = dataSetup.createSampleCsvContent()

    val expectedDocument = dataSetup.createSampleDocument(
      documentUuid = documentUuid,
      documentType = documentType,
      filename = filename,
      fileSize = csvContent.size.toLong(),
    )

    documentManagementApiMockSuccessfulUpload(
      documentType = documentType,
      documentUuid = documentUuid,
      response = expectedDocument,
    )

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = filename,
      fileBytes = csvContent,
      metadata = mapOf("date" to "2026-10-06", "documentSubType" to "CAS1"),
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Success::class.java) { success ->
      val body = success.body as Document
      assertThat(body.documentUuid).isEqualTo(documentUuid)
      assertThat(body.filename).isEqualTo(filename)
    }

    wiremockServer.verify(
      postRequestedFor(urlEqualTo("/documents/$documentType/$documentUuid"))
        .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
        .withAnyRequestBodyPart(
          aMultipart()
            .withName("file")
            .withHeader("Content-Disposition", containing("filename=\"$filename\""))
            .withHeader("Content-Type", containing("text/csv"))
            .withBody(binaryEqualTo(csvContent)),
        )
        .withAnyRequestBodyPart(
          aMultipart()
            .withName("metadata")
            .withHeader("Content-Type", containing("application/json"))
            .withBody(equalToJson("""{"date":"2026-10-06","documentSubType":"CAS1"}""")),
        ),
    )
  }

  @Test
  fun `generateAndUploadAllReports uploads CSV files and metadata for all CAS services`() {
    val executionDate = LocalDate.of(2026, 10, 7)
    val sampleDocument = dataSetup.createSampleDocument()

    documentManagementApiMockSuccessfulUploadAny(
      documentType = "CAS_DOCUMENTS",
      response = sampleDocument,
    )

    casDataScienceReportPublishingService.generateAndUploadAllReports(executionDate)

    listOf("CAS1", "CAS2", "CAS3").forEach { serviceType ->
      val expectedFilename = "${serviceType.lowercase()}_data_science_report_2026-10-07.csv"
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
              .withBody(equalToJson("""{"date":"2026-10-07","documentSubType":"$serviceType"}""")),
          ),
      )
    }
  }

  @Test
  fun `uploadDocument returns Failure StatusCode when server returns 500 error`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val csvContent = dataSetup.createSampleCsvContent()
    val errorResponseBody = """{"status":500,"userMessage":"S3 bucket upload failed"}"""

    documentManagementApiMockUnsuccessfulUpload(
      documentType = documentType,
      documentUuid = documentUuid,
      responseStatus = 500,
      responseBody = errorResponseBody,
    )

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = "test.csv",
      fileBytes = csvContent,
      metadata = mapOf("date" to "2026-10-06"),
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Failure.StatusCode::class.java) { failure ->
      assertThat(failure.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      assertThat(failure.body).isEqualTo(errorResponseBody)
      assertThat(failure.method).isEqualTo(HttpMethod.POST)
      assertThat(failure.path).isEqualTo("/documents/$documentType/$documentUuid")
    }

    wiremockServer.verify(
      postRequestedFor(urlEqualTo("/documents/$documentType/$documentUuid"))
        .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME)),
    )
  }

  @Test
  fun `uploadDocument returns Failure Other when network connection fails`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val csvContent = dataSetup.createSampleCsvContent()

    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        post(urlEqualTo("/documents/$documentType/$documentUuid"))
          .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
          .willReturn(
            aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER),
          ),
      )
    }

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = "test.csv",
      fileBytes = csvContent,
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Failure.Other::class.java) { failure ->
      assertThat(failure.method).isEqualTo(HttpMethod.POST)
      assertThat(failure.path).isEqualTo("/documents/$documentType/$documentUuid")
      assertThat(failure.exception).isNotNull
    }
  }

  @Test
  fun `uploadDocument with custom contentType sends specified Content-Type header`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val filename = "custom_report.json"
    val content = """{"report":"data"}""".toByteArray()

    val expectedDocument = dataSetup.createSampleDocument(
      documentUuid = documentUuid,
      documentType = documentType,
      filename = filename,
      fileSize = content.size.toLong(),
      mimeType = "application/json",
    )

    documentManagementApiMockSuccessfulUpload(
      documentType = documentType,
      documentUuid = documentUuid,
      response = expectedDocument,
    )

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = filename,
      fileBytes = content,
      contentType = org.springframework.http.MediaType.APPLICATION_JSON,
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Success::class.java) { success ->
      val body = success.body as Document
      assertThat(body.documentUuid).isEqualTo(documentUuid)
    }

    wiremockServer.verify(
      postRequestedFor(urlEqualTo("/documents/$documentType/$documentUuid"))
        .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
        .withAnyRequestBodyPart(
          aMultipart()
            .withName("file")
            .withHeader("Content-Disposition", containing("filename=\"$filename\""))
            .withHeader("Content-Type", containing("application/json"))
            .withBody(binaryEqualTo(content)),
        ),
    )
  }

  @Test
  fun `uploadDocument returns Failure Other when server returns 201 with empty response body`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val csvContent = dataSetup.createSampleCsvContent()

    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        post(urlEqualTo("/documents/$documentType/$documentUuid"))
          .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody(""),
          ),
      )
    }

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = "test.csv",
      fileBytes = csvContent,
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Failure.Other::class.java) { failure ->
      assertThat(failure.method).isEqualTo(HttpMethod.POST)
      assertThat(failure.path).isEqualTo("/documents/$documentType/$documentUuid")
      assertThat(failure.exception).isInstanceOf(IllegalArgumentException::class.java)
    }
  }

  @Test
  fun `uploadDocument returns Failure Other when server returns 201 with malformed JSON body`() {
    val documentUuid = UUID.randomUUID()
    val documentType = "CAS_DOCUMENTS"
    val csvContent = dataSetup.createSampleCsvContent()

    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        post(urlEqualTo("/documents/$documentType/$documentUuid"))
          .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(201)
              .withBody("{not-a-valid-json-document}"),
          ),
      )
    }

    val uploadResult = documentManagementApiClient.uploadDocument(
      documentType = documentType,
      documentUuid = documentUuid,
      filename = "test.csv",
      fileBytes = csvContent,
    )

    assertThat(uploadResult).isInstanceOfSatisfying(ClientResult.Failure.Other::class.java) { failure ->
      assertThat(failure.method).isEqualTo(HttpMethod.POST)
      assertThat(failure.path).isEqualTo("/documents/$documentType/$documentUuid")
      assertThat(failure.exception).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException::class.java)
    }
  }
}
