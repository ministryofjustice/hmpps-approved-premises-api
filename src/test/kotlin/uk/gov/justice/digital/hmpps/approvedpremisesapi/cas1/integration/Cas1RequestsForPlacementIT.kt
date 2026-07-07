package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.util.UUID

class Cas1RequestsForPlacementIT : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /cas1/applications/{id}/requests-for-placement/calc/durations")
  inner class GetDurations {

    @Test
    fun `returns 401 without valid JWT`() {
      webTestClient.get()
        .uri("/cas1/applications/${UUID.randomUUID()}/requests-for-placement/calc/durations?apType=${ApType.normal}&sentenceType=${SentenceTypeOption.standardDeterminate}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `returns correct response body for ApType`() {
      val (_, jwt) = givenAUser()

      val response = webTestClient.get()
        .uri("/cas1/applications/${UUID.randomUUID()}/requests-for-placement/calc/durations?apType=${ApType.mhapElliottHouse}&sentenceType=${SentenceTypeOption.standardDeterminate}")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1RequestsForPlacementDurationsCalculationResponseDto>()

      assertThat(response.defaultDurationDays).isEqualTo(84)
      assertThat(response.maxDurationDays).isNull()
    }
  }
}
