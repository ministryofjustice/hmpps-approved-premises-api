package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import java.time.LocalDateTime

@Component
class CaseNotesClient(
  @Qualifier("caseNotesWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  fun getCaseNotesPage(nomsNumber: String, from: LocalDateTime, page: Int, pageSize: Int) = getRequest<CaseNotesPage> {
    path = "/case-notes/$nomsNumber?startDate=$from&page=$page&size=$pageSize"
  }
}
