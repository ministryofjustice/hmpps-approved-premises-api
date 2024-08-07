package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class CaseNotesClient(
  @Qualifier("caseNotesWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  fun getCaseNotesPage(nomsNumber: String, from: LocalDate, page: Int, pageSize: Int) = getRequest<CaseNotesPage> {
    val fromLocalDateTime = LocalDateTime.of(from, LocalTime.MIN)
    path = "/case-notes/$nomsNumber?startDate=$fromLocalDateTime&page=$page&size=$pageSize"
  }
}
