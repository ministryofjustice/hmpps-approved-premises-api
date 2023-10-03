package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.IS_NOT_SUCCESSFUL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.UKBankHolidays

@Component
class GovUKBankHolidaysApiClient(
  @Qualifier("govUKBankHolidaysApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClient, objectMapper, webClientCache) {
  @Cacheable(value = ["ukBankHolidaysCache"], unless = IS_NOT_SUCCESSFUL)
  fun getUKBankHolidays() = getRequest<UKBankHolidays> {
    path = "/bank-holidays.json"
  }
}
