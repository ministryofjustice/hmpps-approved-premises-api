package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationStatusTransformer

class Cas2ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusTransformer: ApplicationStatusTransformer

  @Autowired
  lateinit var statusFinder: Cas2PersistedApplicationStatusFinder

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = objectMapper.writeValueAsString(
      statusFinder.active().map { status -> statusTransformer.transformModelToApi(status) },
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
