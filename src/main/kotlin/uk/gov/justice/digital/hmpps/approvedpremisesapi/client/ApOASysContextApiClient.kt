package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary

@Component
class ApOASysContextApiClient(
  @Qualifier("apOASysContextApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {
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
}
