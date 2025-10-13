package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller.Cas3RefDataType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3ReferenceData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

class Cas3ReferenceDataTest : IntegrationTestBase() {

  @Test
  fun `returns only active reference data for the given type`() {
    val activeCas3BedspaceCharacteristics = cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(3)
    cas3BedspaceCharacteristicEntityFactory.produceAndPersistMultiple(2) {
      withIsActive(false)
    }
    cas3PremisesCharacteristicEntityFactory.produceAndPersistMultiple(6)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()
    val result = webTestClient.get()
      .uri { uriBuilder ->
        uriBuilder.path("cas3/reference-data")
          .queryParam("type", Cas3RefDataType.BEDSPACE_CHARACTERISTICS)
          .build()
      }
      .headers(buildTemporaryAccommodationHeaders(jwt))
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(Cas3ReferenceData::class.java)
      .returnResult()
      .responseBody

    assertThat(result).hasSize(3)
    assertThat(result!!.map { it.id }.sorted()).isEqualTo(activeCas3BedspaceCharacteristics.map { it.id }.sorted())
  }
}
