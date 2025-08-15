package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CaseNotesRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class CaseNotesClient(
  @Qualifier("caseNotesWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getCaseNotesPage(personIdentifier: String, from: LocalDate, page: Int, pageSize: Int) = postRequest<CaseNotesPage> {
    val fromLocalDateTime = LocalDateTime.of(from, LocalTime.MIN)

    path = "/search/case-notes/$personIdentifier"

    body = CaseNotesRequest(
      page = page,
      size = pageSize,
      occurredFrom = fromLocalDateTime,
      includeSensitive = true,
      sort = "occurrenceDateTime,desc",
    )
  }
}
