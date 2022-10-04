package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.LocalDate

@Component
class PrisonsApiClient(
  @Qualifier("prisonsApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  @Cacheable(value = ["inmateDetailsCache"])
  fun getInmateDetails(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
  }

  fun getCaseNotesPage(nomsNumber: String, from: LocalDate, page: Int, pageSize: Int) = getRequest<CaseNotesPage> {
    path = "/api/offenders/$nomsNumber/case-notes/v2?from=$from&page=$page&size=$pageSize"
  }
}
