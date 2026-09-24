package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.activeStatuses
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas2HdcReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusTransformer: Cas2HdcApplicationStatusTransformer

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = jsonMapper.writeValueAsString(
      activeStatuses().map { status -> statusTransformer.transformModelToApi(status) },
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
