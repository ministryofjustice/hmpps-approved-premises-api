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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ReferralDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import java.time.ZonedDateTime

fun IntegrationTestBase.apDeliusContextMockSuccessfulGetReferralDetails(crn: String, bookingId: String, arrivedAt: ZonedDateTime?) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-case/$crn/referrals/$bookingId",
    responseBody = ReferralDetail(
      arrivedAt = arrivedAt,
    ),
  )

fun IntegrationTestBase.apDeliusContextMockSuccessfulStaffMembersCall(staffMember: StaffMember, qCode: String) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/approved-premises/$qCode/staff",
    responseBody = StaffMembersPage(
      content = listOf(staffMember),
    ),
  )

fun IntegrationTestBase.apDeliusContextMockSuccessfulCaseDetailCall(crn: String, response: CaseDetail) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/probation-cases/$crn/details",
    responseBody = response,
  )

fun IntegrationTestBase.apDeliusContextMockSuccessfulStaffDetailsCall(staffCode: String, staffUserDetails: StaffUserDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/secure/staff/staffCode/$staffCode",
    responseBody = staffUserDetails,
  )

fun IntegrationTestBase.apDeliusContextMockSuccessfulTeamsManagingCaseCall(crn: String, response: ManagingTeamsResponse) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/teams/managingCase/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.apDeliusContextMockUserAccess(caseAccess: CaseAccess, username: String = ".*") {
  apDeliusContextAddResponseToUserAccessCall(caseAccess, username)
  apDeliusContextAddSingleResponseToUserAccessCall(caseAccess, username)
}

fun IntegrationTestBase.apDeliusContextAddResponseToUserAccessCall(caseAccess: CaseAccess, username: String = ".*") {
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

fun IntegrationTestBase.apDeliusContextAddSingleResponseToUserAccessCall(caseAccess: CaseAccess, username: String = ".*") {
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

fun IntegrationTestBase.apDeliusContextMockCaseSummary(caseSummary: CaseSummary) {
  this.apDeliusContextAddCaseSummaryToBulkResponse(caseSummary)
  this.apDeliusContextAddSingleCaseSummaryToBulkResponse(caseSummary)
}

fun IntegrationTestBase.apDeliusContextAddCaseSummaryToBulkResponse(caseSummary: CaseSummary) {
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

fun IntegrationTestBase.apDeliusContextAddSingleCaseSummaryToBulkResponse(caseSummary: CaseSummary) {
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

fun IntegrationTestBase.apDeliusContextAddListCaseSummaryToBulkResponse(casesSummary: List<CaseSummary>) {
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
