package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationResponseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.Period
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

    @Nested
    inner class V2 {
      @Test
      fun `returns correct response body for apType mhapElliottHouse, sentence type standardDeterminate`() {
        mockFeatureFlagService.setFlag("use-tier-v3", false)

        val (_, jwt) = givenAUser()
        val application = givenACas1Application(apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE, sentenceType = SentenceTypeOption.ipp.value)
        givenACase(application.crn, tierV2 = TierFactory().produce(), tierV3 = null)

        val response = webTestClient.get()
          .uri("/cas1/applications/${application.id}/requests-for-placement/calc/durations?apType=${ApType.mhapElliottHouse}&sentenceType=${SentenceTypeOption.standardDeterminate}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<Cas1RequestsForPlacementDurationsCalculationResponseDto>()

        assertThat(response.defaultDurationDays).isEqualTo(Period.ofWeeks(12).days)
        assertThat(response.maxDurationDays).isNull()
      }
    }

    @Nested
    inner class V3 {
      @Test
      fun `returns correct response body for apType rfap, sentence type ipp, tier B`() {
        mockFeatureFlagService.setFlag("use-tier-v3", true)

        val (_, jwt) = givenAUser()
        val application = givenACas1Application(apType = ApprovedPremisesType.RFAP, sentenceType = SentenceTypeOption.nonStatutory.value)
        givenACase(application.crn, tierV2 = null, tierV3 = TierFactory().withVersion(TierVersion.V3).withTierScore("B").produce())

        val response = webTestClient.get()
          .uri("/cas1/applications/${application.id}/requests-for-placement/calc/durations?apType=${ApType.rfap}&sentenceType=${SentenceTypeOption.ipp}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .bodyAsObject<Cas1RequestsForPlacementDurationsCalculationResponseDto>()

        assertThat(response.defaultDurationDays).isEqualTo(Period.ofWeeks(16).days)
        assertThat(response.maxDurationDays).isNull()
      }
    }
  }
}
