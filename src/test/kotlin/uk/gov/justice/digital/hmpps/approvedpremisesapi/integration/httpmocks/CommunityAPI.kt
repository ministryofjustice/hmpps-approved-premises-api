package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

fun IntegrationTestBase.communityApiMockSuccessfulStaffUserDetailsCall(staffUserDetails: StaffUserDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/staff/username/${staffUserDetails.username}",
    responseBody = staffUserDetails,
  )

fun IntegrationTestBase.communityApiMockNotFoundStaffUserDetailsCall(username: String) =
  mockUnsuccessfulGetCall(
    url = "/secure/staff/username/$username",
    responseStatus = 404,
  )

fun IntegrationTestBase.communityApiMockSuccessfulOffenderDetailsCall(offenderDetails: OffenderDetailSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/${offenderDetails.otherIds.crn}",
    responseBody = offenderDetails,
  )

fun IntegrationTestBase.communityApiMockServerErrorOffenderDetailsCall(crn: String) =
  mockUnsuccessfulGetCall(
    url = "/secure/offenders/crn/$crn",
    responseStatus = 500,
  )

fun IntegrationTestBase.communityApiMockSuccessfulDocumentsCall(crn: String, groupedDocuments: GroupedDocuments) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/documents/grouped",
    responseBody = groupedDocuments,
  )

fun IntegrationTestBase.communityApiMockSuccessfulDocumentDownloadCall(crn: String, documentId: String, fileContents: ByteArray) =
  mockOAuth2ClientCredentialsCallIfRequired {
    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/documents/$documentId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/octet-stream")
            .withStatus(200)
            .withBody(fileContents),
        ),
    )
  }

fun IntegrationTestBase.communityApiMockNotFoundOffenderDetailsCall(crn: String) =
  mockUnsuccessfulGetCall(
    url = "/secure/offenders/crn/$crn",
    responseStatus = 404,
  )

fun IntegrationTestBase.communityApiMockSuccessfulConvictionsCall(crn: String, response: List<Conviction>) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/convictions",
    responseBody = response,
  )

fun IntegrationTestBase.communityApiMockSuccessfulRegistrationsCall(crn: String, response: Registrations) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/registrations?activeOnly=true",
    responseBody = response,
  )

fun IntegrationTestBase.communityApiMockOffenderUserAccessCall(username: String, crn: String, inclusion: Boolean, exclusion: Boolean) =
  mockOAuth2ClientCredentialsCallIfRequired {
    if (!inclusion && !exclusion) {
      wiremockServer.stubFor(
        WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/user/$username/userAccess"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(200)
              .withBody(
                objectMapper.writeValueAsString(
                  UserOffenderAccess(
                    userRestricted = false,
                    userExcluded = false,
                    restrictionMessage = null,
                  ),
                ),
              ),
          ),
      )
      return@mockOAuth2ClientCredentialsCallIfRequired
    }

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/user/$username/userAccess"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(403)
            .withBody(
              objectMapper.writeValueAsString(
                UserOffenderAccess(
                  userRestricted = inclusion,
                  userExcluded = exclusion,
                  restrictionMessage = null,
                ),
              ),
            ),
        ),
    )
  }
