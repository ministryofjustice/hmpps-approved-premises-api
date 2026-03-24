package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.reporting.model.reference.Cas2v2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas2v2ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusTransformer: ApplicationStatusTransformer

  @Autowired
  lateinit var cas2v2statusFinder: Cas2v2PersistedApplicationStatusFinder

  @Autowired
  lateinit var cas2statusFinder: Cas2PersistedApplicationStatusFinder

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = objectMapper.writeValueAsString(
      cas2v2statusFinder.active().map { status -> statusTransformer.transformModelToApi(status) },
    )

    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    webTestClient.get()
      .uri("/cas2v2/reference-data/application-status")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedStatusOptions)
  }

  @Test
  fun `Ensure CAS2 and CAS2V2 lists are different`() {
    val expectedCas2StatusOptions = objectMapper.writeValueAsString(
      cas2statusFinder.active().map { status -> statusTransformer.transformModelToApi(status) },
    )

    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    webTestClient.get()
      .uri("/cas2v2/reference-data/application-status")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(String::class.java)
      .consumeWith { result ->
        val responseJson = result.responseBody
        assertThat(responseJson).isNotEqualTo(expectedCas2StatusOptions)
      }
  }
}
