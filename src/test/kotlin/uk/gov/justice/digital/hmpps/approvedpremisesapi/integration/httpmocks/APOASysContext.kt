package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary

fun IntegrationTestBase.APOASysContext_mockSuccessfulOffenceDetailsCall(crn: String, response: OffenceDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/offence-details/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.APOASysContext_mockSuccessfulRoSHSummaryCall(crn: String, response: RoshSummary) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/rosh-summary/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.APOASysContext_mockSuccessfulRiskToTheIndividualCall(crn: String, response: RisksToTheIndividual) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/risk-to-the-individual/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.APOASysContext_mockSuccessfulRiskManagementPlanCall(crn: String, response: RiskManagementPlan) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/risk-management-plan/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.APOASysContext_mockSuccessfulNeedsDetailsCall(crn: String, response: NeedsDetails) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/needs-details/$crn",
    responseBody = response,
  )

fun IntegrationTestBase.APOASysContext_mockSuccessfulRoshRatingsCall(crn: String, response: RoshRatings) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/rosh/$crn",
    responseBody = response,
  )
