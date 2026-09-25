package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.external

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenASingleAccommodationServiceClientCredentialsApiCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser

class Cas2ExternalUrlTest : IntegrationTestBase() {

  @Test
  fun `Returns expected cas2 UI template links`() {
    givenAUser { _, _ ->
      givenASingleAccommodationServiceClientCredentialsApiCall { clientCredentialsJwt ->
        webTestClient.get()
          .uri("/cas2/external/url-templates")
          .header("Authorization", "Bearer $clientCredentialsJwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith { result ->
            println(String(result.responseBody ?: ByteArray(0)))
          }
          .jsonPath("$.cas2ApplicationStart").isEqualTo("http://frontend.cas2/applications/start")
      }
    }
  }
}
