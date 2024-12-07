package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.UserOffenderAccess

fun IntegrationTestBase.communityAPIMockSuccessfulOffenderDetailsCall(offenderDetails: OffenderDetailSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/${offenderDetails.otherIds.crn}",
    responseBody = offenderDetails,
  )

fun IntegrationTestBase.communityAPIMockServerErrorOffenderDetailsCall(crn: String) =
  mockUnsuccessfulGetCall(
    url = "/secure/offenders/crn/$crn",
    responseStatus = 500,
  )

fun IntegrationTestBase.communityAPIMockSuccessfulDocumentsCall(crn: String, groupedDocuments: GroupedDocuments) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/documents/grouped",
    responseBody = groupedDocuments,
  )

fun IntegrationTestBase.communityAPIMockSuccessfulDocumentDownloadCall(crn: String, documentId: String, fileContents: ByteArray) =
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

fun IntegrationTestBase.communityAPIMockNotFoundOffenderDetailsCall(crn: String) =
  mockUnsuccessfulGetCall(
    url = "/secure/offenders/crn/$crn",
    responseStatus = 404,
  )

fun IntegrationTestBase.communityAPIMockSuccessfulConvictionsCall(crn: String, response: List<Conviction>) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/convictions",
    responseBody = response,
  )

fun IntegrationTestBase.communityAPIMockSuccessfulRegistrationsCall(crn: String, response: Registrations) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/offenders/crn/$crn/registrations?activeOnly=true",
    responseBody = response,
  )

fun IntegrationTestBase.communityAPIMockOffenderUserAccessCall(username: String, crn: String, inclusion: Boolean, exclusion: Boolean) =
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
