package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.HealthDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class ApAndOASysClient(
  @Qualifier("apOASysContextApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getLatestAssessmentSummary(crn: String) = getRequest<OASysAssessmentSummary> {
    path = "/latest-assessment/$crn"
  }

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
