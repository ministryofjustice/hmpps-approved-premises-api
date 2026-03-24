package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class HealthAndMedicationApiClient(
  @Qualifier("healthAndMedicationApiWebClient") val webClientConfig: WebClientConfig,
  jsonMapper: JsonMapper,
  weClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, jsonMapper, weClientCache) {

  fun getDietAndAllergyDetails(prisonerNumber: String) = getRequest<DietAndAllergyResponse> {
    path = "/prisoners/$prisonerNumber"
  }
}
