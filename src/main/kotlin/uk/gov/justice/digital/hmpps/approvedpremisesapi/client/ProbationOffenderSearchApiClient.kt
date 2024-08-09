package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderSearchNomsRequest

@Component
class ProbationOffenderSearchApiClient(
  @Qualifier("probationOffenderSearchApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun searchOffenderByNomsNumber(nomsNumber: String) = postRequest<List<ProbationOffenderDetail>> {
    path = "/search"
    body = ProbationOffenderSearchNomsRequest(nomsNumber = nomsNumber)
  }
}
