package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.common

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.TierService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UpstreamTierFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_USE_TIER_V3

@ExtendWith(MockKExtension::class)
class TierServiceTest {

  @MockK
  private lateinit var mockHmppsTierApiClient: HMPPSTierApiClient

  @MockK
  private lateinit var mockCaseRepository: CaseRepository

  @MockK
  private lateinit var mockFeatureFlagService: FeatureFlagService

  @InjectMockKs
  private lateinit var tierService: TierService

  @Nested
  inner class GetTier {
    @Test
    fun `should returns the tier from the local case repository if found, preferring V3 if flag is enabled`() {
      val crn = "crn123"
      val normalizedCrn = "CRN123"
      val v2Tier = TierFactory()
        .withTierScore("B2")
        .withVersion(TierVersion.V2)
        .produce()
      val v3Tier = TierFactory()
        .withTierScore("C")
        .withVersion(TierVersion.V3)
        .produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(normalizedCrn)
        .withTierV2(v2Tier)
        .withTierV3(v3Tier)
        .produce()

      every { mockCaseRepository.findByCrn(normalizedCrn) } returns caseEntity
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("C")
      assertThat(result.version).isEqualTo(TierVersionDto.V3)

      verify(exactly = 0) { mockHmppsTierApiClient.getTier(any(), any()) }
    }

    @Test
    fun `should fetch from the API if V3 is missing from local case and flag is enabled`() {
      val crn = "CRN123"
      val v2Tier = TierFactory()
        .withTierScore("B2")
        .withVersion(TierVersion.V2)
        .produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(v2Tier)
        .withTierV3(null)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true

      val upstreamTierV3 = UpstreamTierFactory()
        .withTierScore("C")
        .produce()

      every { mockHmppsTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(HttpStatus.OK, upstreamTierV3)

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("C")
      assertThat(result.version).isEqualTo(TierVersionDto.V3)
    }

    @Test
    fun `should returns V2 from the local case repository if flag is disabled, even if V3 is present`() {
      val crn = "CRN123"
      val v2Tier = TierFactory()
        .withTierScore("B2")
        .withVersion(TierVersion.V2)
        .produce()
      val v3Tier = TierFactory()
        .withTierScore("C")
        .withVersion(TierVersion.V3)
        .produce()
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(v2Tier)
        .withTierV3(v3Tier)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns false

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("B2")
      assertThat(result.version).isEqualTo(TierVersionDto.V2)
    }

    @Test
    fun `should fetches from the API if no local case is found, preferring V3 if flag enabled`() {
      val crn = "CRN123"
      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true

      val upstreamTier = UpstreamTierFactory()
        .withTierScore("C")
        .produce()

      every { mockHmppsTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Success(HttpStatus.OK, upstreamTier)

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("C")
      assertThat(result.version).isEqualTo(TierVersionDto.V3)
    }

    @Test
    fun `should return null if API call for V3 fails and flag is enabled`() {
      val crn = "CRN123"
      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true

      every { mockHmppsTierApiClient.getTier(crn, TierVersion.V3) } returns ClientResult.Failure.StatusCode(
        method = org.springframework.http.HttpMethod.GET,
        path = "/v3/crn/$crn/tier",
        status = HttpStatus.NOT_FOUND,
        body = null,
      )

      val result = tierService.getTier(crn)

      assertThat(result).isNull()
      verify(exactly = 0) { mockHmppsTierApiClient.getTier(crn, TierVersion.V2) }
    }

    @Test
    fun `should only fetches V2 from API if flag is disabled`() {
      val crn = "CRN123"
      every { mockCaseRepository.findByCrn(crn) } returns null
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns false

      val upstreamTierV2 = UpstreamTierFactory()
        .withTierScore("B2")
        .produce()

      every { mockHmppsTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(HttpStatus.OK, upstreamTierV2)

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("B2")
      assertThat(result.version).isEqualTo(TierVersionDto.V2)

      verify(exactly = 0) { mockHmppsTierApiClient.getTier(crn, TierVersion.V3) }
    }

    @Test
    fun `should fetch from the API if V2 is missing from local case and flag is disabled`() {
      val crn = "CRN123"
      val caseEntity = CaseEntityFactory()
        .withCrn(crn)
        .withTierV2(null)
        .produce()

      every { mockCaseRepository.findByCrn(crn) } returns caseEntity
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns false

      val upstreamTierV2 = UpstreamTierFactory()
        .withTierScore("B2")
        .produce()

      every { mockHmppsTierApiClient.getTier(crn, TierVersion.V2) } returns ClientResult.Success(HttpStatus.OK, upstreamTierV2)

      val result = tierService.getTier(crn)

      assertThat(result).isNotNull
      assertThat(result!!.tierScore).isEqualTo("B2")
      assertThat(result.version).isEqualTo(TierVersionDto.V2)
    }
  }

  @Nested
  inner class GetTiers {
    @Test
    fun `returns empty map when no CRNs are provided`() {
      val result = tierService.getTiers(emptySet())

      assertThat(result).isEmpty()

      verify(exactly = 0) { mockCaseRepository.findByCrn(any()) }
      verify(exactly = 0) { mockCaseRepository.findByCrnIn(any()) }
      verify(exactly = 0) { mockHmppsTierApiClient.getTier(any(), any()) }
    }

    @Test
    fun `delegates to getTier and may fetch upstream when a single CRN is provided`() {
      val crn = "crn123"
      val normalizedCrn = "CRN123"

      every { mockCaseRepository.findByCrn(normalizedCrn) } returns null
      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true

      val upstreamTier = UpstreamTierFactory()
        .withTierScore("C")
        .produce()

      every { mockHmppsTierApiClient.getTier(normalizedCrn, TierVersion.V3) } returns ClientResult.Success(HttpStatus.OK, upstreamTier)

      val result = tierService.getTiers(setOf(crn))

      assertThat(result).containsOnlyKeys(normalizedCrn)
      assertThat(result[normalizedCrn]!!.tierScore).isEqualTo("C")
      assertThat(result[normalizedCrn]!!.version).isEqualTo(TierVersionDto.V3)

      verify(exactly = 0) { mockCaseRepository.findByCrnIn(any()) }
    }

    @Test
    fun `only uses the local case cache when more than one CRN is provided, using V3 when the flag is enabled`() {
      val v3Tier = TierFactory()
        .withTierScore("C")
        .withVersion(TierVersion.V3)
        .produce()
      val caseWithTier = CaseEntityFactory()
        .withCrn("CRN1")
        .withTierV3(v3Tier)
        .produce()

      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns true
      every { mockCaseRepository.findByCrnIn(listOf("CRN1", "CRN2")) } returns listOf(caseWithTier)

      val result = tierService.getTiers(setOf("crn1", "crn2"))

      assertThat(result).containsOnlyKeys("CRN1", "CRN2")
      assertThat(result["CRN1"]!!.tierScore).isEqualTo("C")
      assertThat(result["CRN1"]!!.version).isEqualTo(TierVersionDto.V3)
      assertThat(result["CRN2"]).isNull()

      verify(exactly = 0) { mockCaseRepository.findByCrn(any()) }
      verify(exactly = 0) { mockHmppsTierApiClient.getTier(any(), any()) }
    }

    @Test
    fun `only uses the local case cache when more than one CRN is provided, using V2 when the flag is disabled`() {
      val v2Tier = TierFactory()
        .withTierScore("B2")
        .withVersion(TierVersion.V2)
        .produce()
      val v3Tier = TierFactory()
        .withTierScore("C")
        .withVersion(TierVersion.V3)
        .produce()
      val caseWithTier = CaseEntityFactory()
        .withCrn("CRN1")
        .withTierV2(v2Tier)
        .withTierV3(v3Tier)
        .produce()

      every { mockFeatureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3) } returns false
      every { mockCaseRepository.findByCrnIn(listOf("CRN1", "CRN2")) } returns listOf(caseWithTier)

      val result = tierService.getTiers(setOf("crn1", "crn2"))

      assertThat(result["CRN1"]!!.tierScore).isEqualTo("B2")
      assertThat(result["CRN1"]!!.version).isEqualTo(TierVersionDto.V2)
      assertThat(result["CRN2"]).isNull()

      verify(exactly = 0) { mockHmppsTierApiClient.getTier(any(), any()) }
    }
  }
}
