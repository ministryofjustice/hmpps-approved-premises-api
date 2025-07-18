package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary

fun IntegrationTestBase.apOASysContextMockSuccessfulOffenceDetailsCall(crn: String, response: OffenceDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/offence-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockOffenceDetails404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/offence-details/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulRoSHSummaryCall(crn: String, response: RoshSummary) = mockSuccessfulGetCallWithJsonResponse(
  url = "/rosh-summary/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockRoSHSummary404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/rosh-summary/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulRiskToTheIndividualCall(crn: String, response: RisksToTheIndividual) = mockSuccessfulGetCallWithJsonResponse(
  url = "/risk-to-the-individual/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockRiskToTheIndividual404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-to-the-individual/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulRiskManagementPlanCall(crn: String, response: RiskManagementPlan) = mockSuccessfulGetCallWithJsonResponse(
  url = "/risk-management-plan/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockRiskManagementPlan404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-management-plan/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulNeedsDetailsCall(crn: String, response: NeedsDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/needs-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulHealthDetailsCall(crn: String, response: HealthDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/health-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockNeedsDetails404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/needs-details/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apOASysContextMockUnsuccessfulNeedsDetailsCallWithDelay(crn: String, response: NeedsDetails, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/needs-details/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)

fun IntegrationTestBase.apOASysContextMockUnsuccessfulRisksToTheIndividualCallWithDelay(crn: String, response: RisksToTheIndividual, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-to-the-individual/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)

fun IntegrationTestBase.apOASysContextMockSuccessfulRoshRatingsCall(crn: String, response: RoshRatings) = mockSuccessfulGetCallWithJsonResponse(
  url = "/rosh/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apOASysContextMockUnsuccessfulRoshCallWithDelay(crn: String, response: RoshSummary, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/rosh/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)
