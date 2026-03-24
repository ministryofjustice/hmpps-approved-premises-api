package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ApOASysContextApiClient(
  @Qualifier("apOASysContextApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getOffenceDetails(crn: String) = getRequest<OffenceDetails> {
    path = "/offence-details/$crn"
  }

  fun getRiskManagementPlan(crn: String) = getRequest<RiskManagementPlan> {
    path = "/risk-management-plan/$crn"
  }

  fun getRoshSummary(crn: String) = getRequest<RoshSummary> {
    path = "/rosh-summary/$crn"
  }

  fun getRiskToTheIndividual(crn: String) = getRequest<RisksToTheIndividual> {
    path = "/risk-to-the-individual/$crn"
  }

  fun getNeedsDetails(crn: String) = getRequest<NeedsDetails> {
    path = "/needs-details/$crn"
  }

  fun getRoshRatings(crn: String) = getRequest<RoshRatings> {
    path = "/rosh/$crn"
  }

  fun getHealth(crn: String) = getRequest<HealthDetails> {
    path = "/health-details/$crn"
  }
}
