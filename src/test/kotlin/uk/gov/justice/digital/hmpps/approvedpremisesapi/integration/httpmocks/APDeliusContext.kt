package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import com.fasterxml.jackson.core.type.TypeReference
import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ReferralDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import java.time.ZonedDateTime

fun IntegrationTestBase.APDeliusContext_mockSuccessfulGetReferralDetails(crn: String, bookingId: String, arrivedAt: ZonedDateTime?) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-case/$crn/referrals/$bookingId",
    responseBody = ReferralDetail(
      arrivedAt = arrivedAt,
    ),
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulStaffMembersCall(staffMember: StaffMember, qCode: String) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/approved-premises/$qCode/staff",
    responseBody = StaffMembersPage(
      content = listOf(staffMember),
    ),
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulCaseDetailCall(crn: String, response: CaseDetail, responseStatus: Int = 200) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-cases/$crn/details",
    responseBody = response,
    responseStatus = responseStatus,
  )

fun IntegrationTestBase.APDeliusContext_mockUnsuccesfullCaseDetailCall(crn: String, responseStatus: Int) =
  mockUnsuccessfulGetCall(
    url = "/probation-cases/$crn/details",
    responseStatus = responseStatus,
  )

fun IntegrationTestBase.APDeliusContext_mockSuccessfulTeamsManagingCaseCall(crn: String, response: ManagingTeamsResponse) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/teams/managingCase/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.ApDeliusContext_mockUserAccess(caseAccess: CaseAccess, username: String = ".*") {
  ApDeliusContext_addResponseToUserAccessCall(caseAccess, username)
  ApDeliusContext_addSingleResponseToUserAccessCall(caseAccess, username)
}

fun IntegrationTestBase.ApDeliusContext_addResponseToUserAccessCall(caseAccess: CaseAccess, username: String = ".*") {
  val url = "/users/access"
  val existingMock = wiremockServer.listAllStubMappings().mappings.find { it.request.url == url && it.metadata != null && it.metadata.containsKey("bulk") }

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = objectMapper.readValue(existingMock.response.body, UserAccess::class.java)
    val requestBody = objectMapper.readValue(existingMock.request.bodyPatterns[0].expected, object : TypeReference<List<String>>() {}).toMutableList()

    requestBody += caseAccess.crn
    responseBody.access += caseAccess

    editGetStubWithBodyAndJsonResponse(
      url = url,
      uuid = mockId,
      requestBody = WireMock.equalToJson(objectMapper.writeValueAsString(requestBody), true, true),
      responseBody = responseBody,
    ) {
      withQueryParam("username", WireMock.matching(username))
      withMetadata(mapOf("bulk" to Unit))
    }
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
    ) {
      withQueryParam("username", WireMock.matching(username))
      withMetadata(mapOf("bulk" to Unit))
    }
  }
}

fun IntegrationTestBase.ApDeliusContext_addSingleResponseToUserAccessCall(caseAccess: CaseAccess, username: String = ".*") {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/users/access",
    requestBody = WireMock.equalToJson(
      objectMapper.writeValueAsString(
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

fun IntegrationTestBase.ApDeliusContext_mockCaseSummary(caseSummary: CaseSummary) {
  this.ApDeliusContext_addCaseSummaryToBulkResponse(caseSummary)
  this.ApDeliusContext_addSingleCaseSummaryToBulkResponse(caseSummary)
}

fun IntegrationTestBase.ApDeliusContext_addCaseSummaryToBulkResponse(caseSummary: CaseSummary) {
  val url = "/probation-cases/summaries"
  val existingMock = wiremockServer.listAllStubMappings().mappings.find { it.request.url == url && it.metadata != null && it.metadata.containsKey("bulk") }

  if (existingMock != null) {
    val mockId = existingMock.id
    val responseBody = objectMapper.readValue(existingMock.response.body, CaseSummaries::class.java)
    val requestBody = objectMapper.readValue(existingMock.request.bodyPatterns[0].expected, object : TypeReference<List<String>>() {}).toMutableList()

    requestBody += caseSummary.crn
    responseBody.cases += caseSummary

    editGetStubWithBodyAndJsonResponse(
      url = url,
      uuid = mockId,
      requestBody = WireMock.equalToJson(objectMapper.writeValueAsString(requestBody), true, true),
      responseBody = responseBody,
    ) {
      withMetadata(mapOf("bulk" to Unit))
    }
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
    ) {
      withMetadata(mapOf("bulk" to Unit))
    }
  }
}

fun IntegrationTestBase.ApDeliusContext_addSingleCaseSummaryToBulkResponse(caseSummary: CaseSummary) {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/probation-cases/summaries",
    requestBody = WireMock.equalToJson(
      objectMapper.writeValueAsString(
        listOf(caseSummary.crn),
      ),
      false,
      false,
    ),
    responseBody = CaseSummaries(
      cases = listOf(caseSummary),
    ),
  )
}

fun IntegrationTestBase.ApDeliusContext_addListCaseSummaryToBulkResponse(casesSummary: List<CaseSummary>) {
  mockSuccessfulGetCallWithBodyAndJsonResponse(
    url = "/probation-cases/summaries",
    requestBody = WireMock.equalToJson(
      objectMapper.writeValueAsString(
        casesSummary.map { it.crn },
      ),
      true,
      false,
    ),
    responseBody = CaseSummaries(
      cases = casesSummary,
    ),
  )
}
