package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.AvailableTierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.transformer.toDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_USE_TIER_V3
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier as UpstreamTier

@Service
class TierService(
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val caseRepository: CaseRepository,
  private val featureFlagService: FeatureFlagService,
) {

  companion object {
    private val TIERS_V2 = listOf(
      "D0", "D1", "D2", "D3",
      "C0", "C1", "C2", "C3",
      "B0", "B1", "B2", "B3",
      "A0", "A1", "A2", "A3",
    ).map { AvailableTierDto(it) }

    private val TIERS_V3 = listOf(
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "MISSING",
      "NOT_SUPERVISED",
    ).map { AvailableTierDto(it) }
  }

  fun getAvailableTiersV2(): List<AvailableTierDto> = TIERS_V2

  fun getAvailableTiersV3(): List<AvailableTierDto> = TIERS_V3

  fun getTier(crn: String): TierDto? {
    val normalizedCrn = crn.uppercase()
    val useTierV3 = featureFlagService.getBooleanFlag(FEATURE_FLAG_USE_TIER_V3)

    val localTier = getLocalTier(normalizedCrn, useTierV3)
    if (localTier != null) {
      return localTier.toDto()
    }

    val upstreamTier = if (useTierV3) {
      fetchTierOrNull(normalizedCrn, TierVersion.V3)
    } else {
      fetchTierOrNull(normalizedCrn, TierVersion.V2)
    }

    return upstreamTier?.toDto()
  }

  private fun getLocalTier(crn: String, useTierV3: Boolean): Tier? {
    val case = caseRepository.findByCrn(crn) ?: return null

    return if (useTierV3) {
      case.tierV3
    } else {
      case.tierV2
    }
  }

  fun fetchTierOrNull(crn: String, version: TierVersion) = when (val response = hmppsTierApiClient.getTier(crn, version)) {
    is ClientResult.Success -> response.body.toTier(version)
    is ClientResult.Failure -> null
  }

  fun fetchTierOrError(crn: String, version: TierVersion) = when (val response = hmppsTierApiClient.getTier(crn, version)) {
    is ClientResult.Success -> response.body.toTier(version)
    is ClientResult.Failure -> throw response.toException()
  }

  private fun UpstreamTier.toTier(tierVersion: TierVersion) = Tier(
    tierScore = tierScore,
    calculationId = calculationId,
    calculationDate = calculationDate,
    changeReason = changeReason,
    provisional = provisional,
    version = tierVersion,
  )
}
