package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GovUKBankHolidaysApiClient(@Qualifier("govUKBankHolidaysApiClient") webClient: WebClient,
                                 objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {

}