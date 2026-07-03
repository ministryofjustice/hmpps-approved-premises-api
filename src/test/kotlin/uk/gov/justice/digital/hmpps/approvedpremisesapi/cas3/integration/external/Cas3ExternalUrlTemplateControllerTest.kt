package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.external

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser

class Cas3ExternalUrlTemplateControllerTest : Cas3IntegrationTestBase() {

  @Test
  fun `Returns expected cas3 UI template links`() {
    givenAUser { _, _ ->
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas3/external/url-templates")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.cas3ReferralStart").isEqualTo("http://frontend.cas3/applications/start")
      }
    }
  }
}
