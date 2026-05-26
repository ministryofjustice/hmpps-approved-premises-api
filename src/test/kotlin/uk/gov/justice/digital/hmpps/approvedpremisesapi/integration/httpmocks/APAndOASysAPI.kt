package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.apAndOASysMockSuccessfulOffenceDetailsCall(crn: String, response: OffenceDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/offence-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockOffenceDetails404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/offence-details/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulRoSHSummaryCall(crn: String, response: RoshSummary) = mockSuccessfulGetCallWithJsonResponse(
  url = "/rosh-summary/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulRiskToTheIndividualCall(crn: String, response: RisksToTheIndividual) = mockSuccessfulGetCallWithJsonResponse(
  url = "/risk-to-the-individual/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockRiskToTheIndividual404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-to-the-individual/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulRiskManagementPlanCall(crn: String, response: RiskManagementPlan) = mockSuccessfulGetCallWithJsonResponse(
  url = "/risk-management-plan/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockRiskManagementPlan404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-management-plan/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulNeedsDetailsCall(crn: String, response: NeedsDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/needs-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulHealthDetailsCall(crn: String, response: HealthDetails) = mockSuccessfulGetCallWithJsonResponse(
  url = "/health-details/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockHealthDetails404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/health-details/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apAndOASysMockNeedsDetails404Call(crn: String) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/needs-details/$crn",
  responseStatus = 404,
  delayMs = 0,
)

fun IntegrationTestBase.apAndOASysMockUnsuccessfulNeedsDetailsCallWithDelay(crn: String, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/needs-details/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)

fun IntegrationTestBase.apAndOASysMockUnsuccessfulRisksToTheIndividualCallWithDelay(crn: String, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/risk-to-the-individual/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)

fun IntegrationTestBase.apAndOASysMockSuccessfulRoshRatingsCall(crn: String, response: RoshRatings) = mockSuccessfulGetCallWithJsonResponse(
  url = "/rosh/$crn",
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockUnsuccessfulRoshCallWithDelay(crn: String, delayMs: Int) = mockUnsuccessfulGetCallWithDelayedResponse(
  url = "/rosh/$crn",
  responseStatus = 404,
  delayMs = delayMs,
)

fun IntegrationTestBase.apAndOAsysMockAssessmentSummaryResponse(crn: String, response: OASysAssessmentSummary) = mockSuccessfulGetCallWithJsonResponse(
  "/latest-assessment/$crn",
  responseStatus = 200,
  responseBody = response,
)

fun IntegrationTestBase.apAndOASysMockAssessmentSummaryNotFound(crn: String) = mockUnsuccessfulGetCall(
  url = "/latest-assessment/$crn",
  responseStatus = 404,
)
