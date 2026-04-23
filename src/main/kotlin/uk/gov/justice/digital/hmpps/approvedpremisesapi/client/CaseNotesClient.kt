package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class CaseNotesClient(
  @Qualifier("caseNotesWebClient") webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, webClientCache) {
  fun getCaseNotesPage(personIdentifier: String, caseNotesRequest: CaseNotesRequest) = postRequest<CaseNotesPage> {
    path = "/search/case-notes/$personIdentifier"
    body = jsonMapper.writeValueAsString(caseNotesRequest)
    headers = HttpHeaders().apply { set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) }
  }
}
