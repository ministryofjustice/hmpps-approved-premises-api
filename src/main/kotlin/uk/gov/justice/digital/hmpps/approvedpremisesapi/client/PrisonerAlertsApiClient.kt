package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.AlertsPage

@Component
class PrisonerAlertsApiClient(
  @Qualifier("prisonerAlertsApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getAlerts(nomsNumber: String, alertCode: String) = getRequest<AlertsPage> {
    path = "/prisoners/$nomsNumber/alerts?alertCode=$alertCode&sort=createdAt,DESC"
  }
}
