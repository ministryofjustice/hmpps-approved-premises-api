package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier as UpstreamTier

@Service
class CaseService(
  private val caseRepository: CaseRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val featureFlagService: FeatureFlagService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val includeTierV3: Boolean
    get() = featureFlagService.getBooleanFlag("include-tier-v3")

  fun ensureCaseExists(crn: String): CaseEntity {
    val caseSummary = getCaseSummary(crn)
    val tiers = fetchAvailableTiers(crn)

    val caseEntity = caseRepository.findByCrn(caseSummary.crn)
      ?.updateFrom(caseSummary, tiers)
      ?: newCaseEntity(caseSummary, tiers)

    return caseRepository.saveAndFlush(caseEntity)
  }

  fun reviseTier(crn: String): Boolean {
    val case = caseRepository.findByCrn(crn) ?: return false

    case.tierV2 = fetchTierOrError(crn, TierVersion.V2)
    log.info("Have updated tierV2 for $crn to $case.tierV2")

    if (includeTierV3) {
      case.tierV3 = fetchTierOrError(crn, TierVersion.V3)
      log.info("Have updated tierV3 for $crn to $case.tierV3")
    }
    caseRepository.save(case)
    return true
  }

  fun getCase(crn: String): CaseDto? = caseRepository.findByCrn(crn)?.toDto()

  /**
   * If a case can't be found there will be no `CaseDto` representation will be included in the result
   */
  fun getCases(crns: List<String>): List<CaseDto> = caseRepository.findByCrnIn(crns).map { it.toDto() }

  private data class CaseTiers(
    val v2: Tier?,
    val v3: Tier?,
  )

  private fun fetchAvailableTiers(crn: String) = CaseTiers(
    v2 = fetchTierOrNull(crn, TierVersion.V2),
    v3 = if (includeTierV3) fetchTierOrNull(crn, TierVersion.V3) else null,
  )

  private fun CaseEntity.updateFrom(caseSummary: CaseSummary, tiers: CaseTiers): CaseEntity = apply {
    name = "${caseSummary.name.forename} ${caseSummary.name.surname}"
      .uppercase()
      .trim()
    nomsNumber = caseSummary.nomsId
    lastUpdatedAt = OffsetDateTime.now()
    tierV2 = tiers.v2
    tierV3 = tiers.v3
  }

  private fun newCaseEntity(caseSummary: CaseSummary, tiers: CaseTiers) = CaseEntity(
    id = UUID.randomUUID(),
    crn = caseSummary.crn,
    createdAt = OffsetDateTime.now(),
    lastUpdatedAt = OffsetDateTime.now(),
    name = "${caseSummary.name.forename} ${caseSummary.name.surname}"
      .uppercase()
      .trim(),
    nomsNumber = caseSummary.nomsId,
    tierV2 = tiers.v2,
    tierV3 = tiers.v3,
  )

  private fun CaseEntity.toDto() = CaseDto(
    crn = crn,
    nomsNumber = nomsNumber,
    name = name,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    tier = tierV2?.toDto(),
  )

  private fun Tier.toDto() = TierDto(
    tierScore = tierScore,
    calculationDate = calculationDate,
    provisional = provisional,
    version = TierVersionDto.valueOf(version.name),
  )

  private fun UpstreamTier.toTier(tierVersion: TierVersion) = Tier(
    tierScore = tierScore,
    calculationId = calculationId,
    calculationDate = calculationDate,
    changeReason = changeReason,
    provisional = provisional,
    version = tierVersion,
  )

  private fun fetchTierOrNull(crn: String, version: TierVersion) = when (val response = hmppsTierApiClient.getTier(crn, version)) {
    is ClientResult.Success -> response.body.toTier(version)
    is ClientResult.Failure -> null
  }

  private fun fetchTierOrError(crn: String, version: TierVersion) = when (val response = hmppsTierApiClient.getTier(crn, version)) {
    is ClientResult.Success -> response.body.toTier(version)
    is ClientResult.Failure -> throw response.toException()
  }

  private fun getCaseSummary(crn: String): CaseSummary = when (
    val caseSummariesResponse = apDeliusContextApiClient.getCaseSummaries(listOf(crn))
  ) {
    is ClientResult.Success ->
      caseSummariesResponse.body.cases.firstOrNull() ?: throw NotFoundProblem(crn, "Offender")

    is ClientResult.Failure -> caseSummariesResponse.throwException()
  }
}
