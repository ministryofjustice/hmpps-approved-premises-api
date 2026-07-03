package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.external

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser

class Cas1ExternalUrlTemplateControllerTest : IntegrationTestBase() {

  @Test
  fun `Get all referrals returns ok`() {
    givenAUser { _, _ ->
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas1/external/url-templates")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith { result ->
            println(String(result.responseBody ?: ByteArray(0)))
          }
          .jsonPath("$.cas1ApplicationStart").isEqualTo("http://frontend.cas1/applications/start")
      }
    }
  }
}
