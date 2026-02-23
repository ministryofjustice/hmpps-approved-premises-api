package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig

@Component
class HealthAndMedicationsApiClient(
  @Qualifier("healthAndMedicationApiWebClient") val webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  weClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, weClientCache) {

  fun getDietAndAllergyDetails(prisonerNumber: String) = getRequest<DietAndAllergyResponse> {
    path = "/prisoners/$prisonerNumber"
  }
}
