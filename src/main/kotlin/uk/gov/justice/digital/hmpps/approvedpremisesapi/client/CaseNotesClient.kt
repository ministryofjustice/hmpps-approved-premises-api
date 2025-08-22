package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class CaseNotesClient(
  @Qualifier("caseNotesWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getCaseNotesPage(personIdentifier: String, caseNotesRequest: CaseNotesRequest) = postRequest<CaseNotesPage> {
    path = "/search/case-notes/$personIdentifier"
    body = objectMapper.writeValueAsString(caseNotesRequest)
    headers = HttpHeaders().apply { set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) }
  }
}
