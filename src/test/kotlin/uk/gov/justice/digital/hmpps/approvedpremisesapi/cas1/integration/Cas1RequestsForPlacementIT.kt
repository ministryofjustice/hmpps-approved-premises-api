package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.factory.Cas1RequestsForPlacementDurationsCalculationRequestDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject

class Cas1RequestsForPlacementIT : IntegrationTestBase() {

  @Nested
  @DisplayName("POST /cas1/requests-for-placement/calc/durations")
  inner class GetDurations {

    @Test
    fun `returns 401 without valid JWT`() {
      val requestDto = Cas1RequestsForPlacementDurationsCalculationRequestDtoFactory().produce()

      webTestClient.post()
        .uri("/cas1/requests-for-placement/calc/durations")
        .bodyValue(requestDto)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `returns error if tier has v3 version`() {
      val (_, jwt) = givenAUser()

      val requestDto = Cas1RequestsForPlacementDurationsCalculationRequestDtoFactory()
        .withApType(ApType.mhapElliottHouse)
        .withTier(TierDtoFactory().withVersion(TierVersionDto.V3).produce())
        .produce()

      val response = webTestClient.post()
        .uri("/cas1/requests-for-placement/calc/durations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          requestDto,
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .bodyAsObject<ValidationError>()

      assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(response.detail).isEqualTo("Tier version V3 is not supported for duration calculations")
    }

    @Test
    fun `returns correct response body if type is ApType`() {
      val (_, jwt) = givenAUser()

      val requestDto = Cas1RequestsForPlacementDurationsCalculationRequestDtoFactory()
        .withApType(ApType.mhapElliottHouse)
        .withTier(TierDtoFactory().withVersion(TierVersionDto.V2).produce())
        .produce()

      val response = webTestClient.post()
        .uri("/cas1/requests-for-placement/calc/durations")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          requestDto,
        )
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1RequestsForPlacementDurationsCalculationResponseDto>()

      assertThat(response.defaultDurationDays).isEqualTo(84)
      assertThat(response.maxDurationDays).isNull()
    }
  }
}
