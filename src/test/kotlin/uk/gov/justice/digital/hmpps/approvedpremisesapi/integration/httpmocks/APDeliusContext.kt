package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import java.util.UUID

fun IntegrationTestBase.apDeliusContextMockSuccessfulStaffMembersCall(staffMember: StaffMember, qCode: String) {
  mockSuccessfulGetCallWithJsonResponse(
    url = "/approved-premises/$qCode/staff",
    responseBody = StaffMembersPage(
      content = listOf(staffMember),
    ),
  )

  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/staff/staffCode/${staffMember.code}",
    responseBody = staffMember,
  )

  mockSuccessfulGetCallWithJsonResponse(
    url = "/staff?staffCode=${staffMember.code}",
    responseBody = staffMember,
  )
}

fun IntegrationTestBase.apDeliusContextMockSuccessfulStaffDetailByCodeCall(staffDetail: StaffDetail) {
  mockSuccessfulGetCallWithJsonResponse(
    url = "/staff?code=${staffDetail.code}",
    responseBody = staffDetail,
  )
}

fun IntegrationTestBase.apDeliusContextMockSuccessfulCaseDetailCall(crn: String, response: CaseDetail, responseStatus: Int = 200) = mockSuccessfulGetCallWithJsonResponse(
  url = "/probation-cases/$crn/details",
  responseBody = response,
  responseStatus = responseStatus,
)

fun IntegrationTestBase.apDeliusContextMockUnsuccesfullCaseDetailCall(crn: String, responseStatus: Int) = mockUnsuccessfulGetCall(
  url = "/probation-cases/$crn/details",
  responseStatus = responseStatus,
)

fun IntegrationTestBase.apDeliusContextMockSuccessfulTeamsManagingCaseCall(crn: String, response: ManagingTeamsResponse) = mockSuccessfulGetCallWithJsonResponse(
  url = "/teams/managingCase/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apDeliusContextMockUserAccess(caseAccess: CaseAccess, username: String = ".*") {
  apDeliusContextUserAccessAddCase(listOf(caseAccess), username)
  apDeliusContextUserAccessSingleCase(caseAccess, username)
}

fun IntegrationTestBase.apDeliusContextUserAccessAddCase(casesAccess: List<CaseAccess>, username: String = ".*") {
  val metadata = "apDeliusContextAddResponseToUserAccessCall-$username"
  val url = "/users/access"
  // we use anything pattern because it's very difficult to accurately determine the
  // list of CRNs provided for a given bulk response when the bulk response is being
  // built on-the-fly
  val requestBodyPattern = AnythingPattern()
  val existingMock = findStubMappingByMetadataKey(metadata)

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = jsonMapper.readValue(existingMock.response.body, UserAccess::class.java)

    responseBody.access += casesAccess

    editGetStubWithBodyAndJsonResponse(
      url = url,
      uuid = mockId,
      requestBody = requestBodyPattern,
      responseBody = responseBody,
    ) {
      withQueryParam("username", WireMock.matching(username))
      withMetadata(mapOf(metadata to Unit))
    }
  } else {
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = url,
      requestBody = requestBodyPattern,
      responseBody = UserAccess(
        access = casesAccess,
      ),
    ) {
      withQueryParam("username", WireMock.matching(username))
      withMetadata(mapOf(metadata to Unit))
    }
  }
}

fun IntegrationTestBase.apDeliusContextUserAccessSingleCase(caseAccess: CaseAccess, username: String = ".*") {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/users/access",
    requestBody = WireMock.equalToJson(
      jsonMapper.writeValueAsString(
        listOf(caseAccess.crn),
      ),
      false,
      false,
    ),
    responseBody = UserAccess(
      access = listOf(caseAccess),
    ),
  ) {
    withQueryParam("username", WireMock.matching(username))
  }
}

fun IntegrationTestBase.apDeliusContextCaseSummariesAddCase(caseSummary: CaseSummary) {
  val metadata = "apDeliusContextAddCaseSummaryToBulkResponse"
  val url = "/probation-cases/summaries"
  // we use anything pattern because it's very difficult to accurately determine the
  // list of CRNs provided for a given bulk response when the bulk response is being
  // built on-the-fly
  val requestBodyPattern = AnythingPattern()
  val existingMock = findStubMappingByMetadataKey(metadata)

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = jsonMapper.readValue(existingMock.response.body, CaseSummaries::class.java)
    responseBody.cases += caseSummary

    editGetStubWithBodyAndJsonResponse(
      url = url,
      uuid = mockId,
      requestBody = requestBodyPattern,
      responseBody = responseBody,
    ) {
      withMetadata(mapOf(metadata to Unit))
    }
  } else {
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = url,
      requestBody = requestBodyPattern,
      responseBody = CaseSummaries(
        cases = listOf(caseSummary),
      ),
    ) {
      withMetadata(mapOf(metadata to Unit))
    }
  }
}

fun IntegrationTestBase.apDeliusContextCaseSummariesSingleCase(caseSummary: CaseSummary) {

  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/probation-cases/summaries",
    requestBody = WireMock.equalToJson(
      jsonMapper.writeValueAsString(
        listOf(caseSummary.crn),
      ),
      false,
      false,
    ),
    responseBody = CaseSummaries(
      cases = listOf(caseSummary),
    ),
  )
  caseSummary.nomsId?.let {
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = "/probation-cases/summaries",
      requestBody = WireMock.equalToJson(
        jsonMapper.writeValueAsString(
          listOf(caseSummary.nomsId),
        ),
        false,
        false,
      ),
      responseBody = CaseSummaries(
        cases = listOf(caseSummary),
      ),
    )
  }
}

fun IntegrationTestBase.apDeliusContextCaseSummariesMultipleCases(
  casesSummary: List<CaseSummary>,
  crns: List<String> = casesSummary.map { it.crn },
) {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/probation-cases/summaries",
    requestBody = WireMock.equalToJson(
      jsonMapper.writeValueAsString(crns),
      true,
      false,
    ),
    responseBody = CaseSummaries(
      cases = casesSummary,
    ),
  )
  casesSummary.mapNotNull { it.nomsId }.takeIf { it.isNotEmpty() }.let {
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = "/probation-cases/summaries",
      requestBody = WireMock.equalToJson(
        jsonMapper.writeValueAsString(it),
        true,
        false,
      ),
      responseBody = CaseSummaries(
        cases = casesSummary,
      ),
    )
  }
}

fun IntegrationTestBase.apDeliusContextCaseSummariesEmptyResponseForCrn(crn: String) {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/probation-cases/summaries",
    requestBody = WireMock.equalToJson(
      jsonMapper.writeValueAsString(
        listOf(crn),
      ),
      false,
      false,
    ),
    responseBody = CaseSummaries(
      cases = emptyList(),
    ),
  )
}

fun IntegrationTestBase.apDeliusContextCaseSummariesErrorResponse(responseStatus: Int = 500) = mockUnsuccessfulGetCall(
  url = "/probation-cases/summaries",
  responseStatus = responseStatus,
)

fun IntegrationTestBase.apDeliusContextAddStaffDetailResponse(staffDetail: StaffDetail) {
  mockSuccessfulGetCallWithJsonResponse(
    url = "/staff/${staffDetail.username}",
    responseBody = staffDetail,
  )

  mockSuccessfulGetCallWithJsonResponse(
    url = "/staff?code=${staffDetail.code}",
    responseBody = staffDetail,
  )
}

fun IntegrationTestBase.apDeliusContextMockNotFoundStaffDetailByStaffCodeCall(staffCode: String) = mockUnsuccessfulGetCall(
  url = "/staff?code=$staffCode",
  responseStatus = 404,
)

fun IntegrationTestBase.apDeliusContextMockNotFoundStaffDetailCall(username: String) = mockUnsuccessfulGetCall(
  url = "/staff/$username",
  responseStatus = 404,
)

fun IntegrationTestBase.apDeliusContextMockSuccessfulDocumentsCall(crn: String, documents: List<APDeliusDocument>) = mockSuccessfulGetCallWithJsonResponse(
  url = "/documents/$crn/all",
  responseBody = documents,
)

fun IntegrationTestBase.apDeliusContextMockSuccessfulDocumentDownloadCall(crn: String, documentId: UUID, fileContents: ByteArray) = mockOAuth2ClientCredentialsCallIfRequired {
  wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/documents/$crn/$documentId"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/octet-stream")
          .withStatus(200)
          .withBody(fileContents),
      ),
  )
}

private fun IntegrationTestBase.findStubMappingByMetadataKey(key: String) = wiremockServer.listAllStubMappings().mappings.find { it.metadata?.containsKey(key) == true }
