package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.adjudications.AdjudicationsPage

@Component
class AdjudicationsApiClient(
  @Qualifier("adjudicationsApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {

  fun getAdjudicationsPage(nomsNumber: String, page: Int, pageSize: Int) = getRequest<AdjudicationsPage> {
    path = "/adjudications/$nomsNumber/adjudications?page=$page&size=$pageSize"
  }
}
