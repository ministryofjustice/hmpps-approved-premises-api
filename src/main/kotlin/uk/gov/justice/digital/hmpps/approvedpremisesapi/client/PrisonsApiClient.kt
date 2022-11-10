package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.LocalDate

@Component
class PrisonsApiClient(
  @Qualifier("prisonsApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  @Cacheable(value = ["inmateDetailsCache"], unless = IS_NOT_SUCCESSFUL)
  fun getInmateDetails(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
  }

  fun getCaseNotesPage(nomsNumber: String, from: LocalDate, page: Int, pageSize: Int) = getRequest<CaseNotesPage> {
    path = "/api/offenders/$nomsNumber/case-notes/v2?from=$from&page=$page&size=$pageSize"
  }

  fun getAdjudicationsPage(nomsNumber: String, offset: Int?, pageSize: Int) = getRequest<AdjudicationsPage> {
    withHeader("Page-Limit", pageSize.toString())

    if (offset != null) {
      withHeader("Page-Offset", offset.toString())
    }

    path = "/api/offenders/$nomsNumber/adjudications"
  }

  fun getAlerts(nomsNumber: String, alertCode: String) = getRequest<List<Alert>> {
    path = "/api/offenders/$nomsNumber/alerts/v2?alertCodes=HA&sort=dateCreated&direction=DESC"
  }
}
