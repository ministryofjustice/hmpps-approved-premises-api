package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class AdjudicationsApiClient(
  @Qualifier("adjudicationsApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getAdjudicationsPage(nomsNumber: String, offset: Int?, pageSize: Int) = getRequest<AdjudicationsPage> {
    withHeader("Page-Limit", pageSize.toString())

    if (offset != null) {
      withHeader("Page-Offset", offset.toString())
    }

    path = "/reported-adjudications/prisoner/$nomsNumber"
  }
}
