package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding.activeStatuses
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas2ReferenceDataTest : IntegrationTestBase() {

  @Autowired
  lateinit var statusTransformer: Cas2HdcApplicationStatusTransformer

  @Test
  fun `All available application status options are returned`() {
    val expectedStatusOptions = jsonMapper.writeValueAsString(
      activeStatuses().map { status -> statusTransformer.transformV2ModelToApi(status) },
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

  @Test
  fun `Ensure CAS2 and CAS2V2 lists are different`() {
    val expectedCas2StatusOptions = jsonMapper.writeValueAsString(
      activeStatuses().map { status -> statusTransformer.transformModelToApi(status) },
    )

    val jwt = jwtAuthHelper.createValidExternalAuthorisationCodeJwt()

    webTestClient.get()
      .uri("/cas2/reference-data/application-status")
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
