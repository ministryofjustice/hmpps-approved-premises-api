package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess

fun IntegrationTestBase.APDeliusContext_mockSuccessfulStaffMembersCall(staffMember: StaffMember, qCode: String) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/approved-premises/$qCode/staff",
    responseBody = StaffMembersPage(
      content = listOf(staffMember),
    ),
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulCaseDetailCall(crn: String, response: CaseDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-cases/$crn/details",
    responseBody = response,
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulCaseSummaryCall(crns: List<String>, response: CaseSummaries) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-cases/summaries",
    responseBody = response,
    requestBody = crns,
  )

fun IntegrationTestBase.APDeliusContext_mockUserAccessCall(crns: List<String>, deliusUsername: String, userAccess: UserAccess) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/users/access?username=$deliusUsername",
    responseBody = userAccess,
    requestBody = crns,
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulStaffDetailsCall(staffCode: String, staffUserDetails: StaffUserDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/staff/staffCode/$staffCode",
    responseBody = staffUserDetails,
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulTeamsManagingCaseCall(crn: String, response: ManagingTeamsResponse) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/teams/managingCase/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.ApDeliusContext_addResponseToUserAccessCall(caseAccess: CaseAccess, username: String = "\${json-unit.any-string}") {
  val url = "/users/access?username=$username"
  val existingMock = wiremockServer.listAllStubMappings().mappings.find { it.request.url == url }

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = objectMapper.readValue(existingMock.response.body, UserAccess::class.java)
    val requestBody = objectMapper.readValue(existingMock.request.bodyPatterns[0].expected, object : TypeReference<List<String>>() {}).toMutableList()

    requestBody += caseAccess.crn
    responseBody.access += caseAccess

    editGetStubWithBodyAndJsonResponse(url, mockId, WireMock.equalToJson(objectMapper.writeValueAsString(requestBody), true, true), responseBody)
  } else {
    val requestBody = listOf(caseAccess.crn)
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = url,
      requestBody = WireMock.equalToJson(
        objectMapper.writeValueAsString(
          requestBody,
        ),
        true,
        true,
      ),
      responseBody = UserAccess(
        access = listOf(caseAccess),
      ),
    )
  }
}

fun IntegrationTestBase.ApDeliusContext_addCaseSummaryToBulkResponse(caseSummary: CaseSummary) {
  val url = "/probation-cases/summaries"
  val existingMock = wiremockServer.listAllStubMappings().mappings.find { it.request.url == url }

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = objectMapper.readValue(existingMock.response.body, CaseSummaries::class.java)
    val requestBody = objectMapper.readValue(existingMock.request.bodyPatterns[0].expected, object : TypeReference<List<String>>() {}).toMutableList()

    requestBody += caseSummary.crn
    responseBody.cases += caseSummary

    editGetStubWithBodyAndJsonResponse(url, mockId, WireMock.equalToJson(objectMapper.writeValueAsString(requestBody), true, true), responseBody)
  } else {
    mockSuccessfulGetCallWithBodyAndJsonResponse(
      url = url,
      requestBody = WireMock.equalToJson(
        objectMapper.writeValueAsString(
          listOf(caseSummary.crn),
        ),
        true,
        true,
      ),
      responseBody = CaseSummaries(
        cases = listOf(caseSummary),
      ),
    )
  }
}
