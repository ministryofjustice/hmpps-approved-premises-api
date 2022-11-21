package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan

@Component
class ApOASysContextApiClient(
  @Qualifier("apOASysContextApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  fun getOffenceDetails(crn: String) = getRequest<OffenceDetails> {
    path = "/offence-details/$crn"
  }

  fun getRiskManagementPlan(crn: String) = getRequest<RiskManagementPlan> {
    path = "/risk-management-plan/$crn"
  }
}
