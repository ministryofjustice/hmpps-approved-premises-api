package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class PrisonRegisterClient(
  @Qualifier("prisonerSearchWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {

  fun getPrison(prisonId: String) = getRequest<PrisonDto> {
    path = "/prisons/id/$prisonId"
  }

  fun getOmuContactDetails(prisonId: String) = getRequest<OmuContactDetails> {
    path = "/secure/prisons/id/$prisonId/department/contact-details?departmentType=OFFENDER_MANAGEMENT_UNIT"
  }
}

data class PrisonDto(
  val prisonName: String,
  val prisonId: String,
)

data class OmuContactDetails(
  val emailAddress: String?,
)
