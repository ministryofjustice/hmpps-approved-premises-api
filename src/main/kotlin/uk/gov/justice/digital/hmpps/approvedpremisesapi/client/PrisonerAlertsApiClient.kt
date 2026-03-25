package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.AlertsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class PrisonerAlertsApiClient(
  @Qualifier("prisonerAlertsApiWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getAlerts(nomsNumber: String, alertCode: String) = getRequest<AlertsPage> {
    path = "/prisoners/$nomsNumber/alerts?alertCode=$alertCode&sort=createdAt,DESC"
  }
}
