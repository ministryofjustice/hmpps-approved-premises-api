package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.StaffUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage

fun IntegrationTestBase.APDeliusContext_mockSuccessfulStaffMembersCall(staffMember: StaffMember, qCode: String) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/approved-premises/$qCode/staff",
    responseBody = StaffMembersPage(
      content = listOf(staffMember),
    ),
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
