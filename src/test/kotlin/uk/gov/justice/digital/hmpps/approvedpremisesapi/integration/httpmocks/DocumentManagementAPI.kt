package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.DocumentManagementApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.documentmanagement.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.util.UUID

fun IntegrationTestBase.documentManagementApiMockSuccessfulUpload(
  documentType: String,
  documentUuid: UUID,
  response: Document,
  status: Int = 201,
) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    post(urlEqualTo("/documents/$documentType/$documentUuid"))
      .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
          .withBody(jsonMapper.writeValueAsString(response)),
      ),
  )
}

fun IntegrationTestBase.documentManagementApiMockSuccessfulUploadAny(
  documentType: String = "CAS_DOCUMENTS",
  response: Document,
  status: Int = 201,
) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    post(urlPathMatching("/documents/$documentType/.*"))
      .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
          .withBody(jsonMapper.writeValueAsString(response)),
      ),
  )
}

fun IntegrationTestBase.documentManagementApiMockSuccessfulGetDocument(
  documentUuid: UUID,
  response: Document,
  status: Int = 200,
) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    WireMock.get(urlEqualTo("/documents/$documentUuid"))
      .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
          .withBody(jsonMapper.writeValueAsString(response)),
      ),
  )
}

fun IntegrationTestBase.documentManagementApiMockUnsuccessfulUpload(
  documentType: String,
  documentUuid: UUID,
  responseStatus: Int = 500,
  responseBody: String? = null,
) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    post(urlEqualTo("/documents/$documentType/$documentUuid"))
      .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
      .willReturn(
        aResponse()
          .withStatus(responseStatus)
          .apply {
            if (responseBody != null) {
              withHeader("Content-Type", "application/json")
              withBody(responseBody)
            }
          },
      ),
  )
}

fun IntegrationTestBase.documentManagementApiMockUnsuccessfulUploadAny(
  documentType: String = "CAS_DOCUMENTS",
  responseStatus: Int = 500,
  responseBody: String? = null,
) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    post(urlPathMatching("/documents/$documentType/.*"))
      .withHeader(DocumentManagementApiClient.SERVICE_NAME_HEADER, equalTo(DocumentManagementApiClient.APPROVED_PREMISES_SERVICE_NAME))
      .willReturn(
        aResponse()
          .withStatus(responseStatus)
          .apply {
            if (responseBody != null) {
              withHeader("Content-Type", "application/json")
              withBody(responseBody)
            }
          },
      ),
  )
}
