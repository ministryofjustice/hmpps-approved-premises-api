package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails

fun IntegrationTestBase.CommunityAPI_mockSuccessfulStaffUserDetailsCall(staffUserDetails: StaffUserDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/staff/username/${staffUserDetails.username}",
    responseBody = staffUserDetails
  )

fun IntegrationTestBase.CommunityAPI_mockSuccessfulOffenderDetailsCall(offenderDetails: OffenderDetailSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/${offenderDetails.otherIds.crn}",
    responseBody = offenderDetails
  )

fun IntegrationTestBase.CommunityAPI_mockSuccessfulDocumentsCall(crn: String, groupedDocuments: GroupedDocuments) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/documents/grouped",
    responseBody = groupedDocuments
  )

fun IntegrationTestBase.CommunityAPI_mockSuccessfulDocumentDownloadCall(crn: String, documentId: String, fileContents: ByteArray) =
  mockOAuth2ClientCredentialsCallIfRequired {
    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/documents/$documentId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/octet-stream")
            .withStatus(200)
            .withBody(fileContents)
        )
    )
  }
