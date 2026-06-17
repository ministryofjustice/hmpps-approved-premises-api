package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcPersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas2ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusTransformer: Cas2HdcApplicationStatusTransformer

  @Autowired
  lateinit var statusFinder: Cas2HdcPersistedApplicationStatusFinder

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = jsonMapper.writeValueAsString(
      statusFinder.active().map { status -> statusTransformer.transformModelToApi(status) },
    )

    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    webTestClient.get()
      .uri("/cas2-hdc/reference-data/application-status")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedStatusOptions)
  }
}
