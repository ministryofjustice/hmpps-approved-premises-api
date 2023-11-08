package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding

class Cas2ReferenceDataTest : IntegrationTestBase() {

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = objectMapper.writeValueAsString(
      Cas2ApplicationStatusSeeding.statusList(),
    )

    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    webTestClient.get()
      .uri("/cas2/reference-data/application-status")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedStatusOptions)
  }
}
