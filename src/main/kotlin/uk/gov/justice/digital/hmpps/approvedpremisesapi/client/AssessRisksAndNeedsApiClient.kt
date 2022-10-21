package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Needs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.RoshRisks

@Component
class AssessRisksAndNeedsApiClient(
  @Qualifier("assessRisksAndNeedsApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper
) : BaseHMPPSClient(webClient, objectMapper) {
  fun getRoshRisks(crn: String, jwt: String) = getRequest<RoshRisks> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/risks/crn/$crn"
  }

  fun getNeeds(crn: String, jwt: String) = getRequest<Needs> {
    withHeader("Authorization", "Bearer $jwt")
    path = "/needs/crn/$crn"
  }
}
