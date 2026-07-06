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
import java.time.OffsetDateTime
import java.util.UUID

@Service
class CaseService(
  private val caseRepository: CaseRepository,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun ensureCaseExists(crn: String): CaseEntity {
    val caseSummary = getCaseSummary(crn)
    val riskTier = getRiskTierOrNull(crn)

    val fullName =
      "${caseSummary.name.forename} ${caseSummary.name.surname}"
        .uppercase()
        .trim()

    fun toTier() = riskTier?.toTier()

    val existing = caseRepository.findByCrn(caseSummary.crn)

    val caseEntity = existing?.apply {
      name = fullName
      nomsNumber = caseSummary.nomsId
      tierV2 = toTier()
    } ?: CaseEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn,
      name = fullName,
      nomsNumber = caseSummary.nomsId,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = OffsetDateTime.now(),
      tierV2 = toTier(),
    )

    return caseRepository.saveAndFlush(caseEntity)
  }

  fun getCase(crn: String): CaseDto? = caseRepository.findByCrn(crn)?.let {
    CaseDto(
      crn = it.crn,
      nomsNumber = it.nomsNumber,
      name = it.name,
      createdAt = it.createdAt,
      lastUpdatedAt = it.lastUpdatedAt,
      tier = it.tierV2?.let {
        TierDto(
          tierScore = it.tierScore,
          calculationDate = it.calculationDate,
          provisional = it.provisional,
          version = TierVersionDto.valueOf(it.version.name),
        )
      },
    )
  }

  fun reviseTier(crn: String): Boolean {
    val case = caseRepository.findByCrn(crn) ?: return false

    val tier = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> tierResponse.body
      is ClientResult.Failure -> throw tierResponse.toException()
    }

    case.tierV2 = tier.toTier()

    caseRepository.save(case)

    log.info("Have updated tier for $crn to $tier")
    return true
  }

  private fun uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier.toTier() = Tier(
    tierScore = tierScore,
    calculationId = calculationId,
    calculationDate = calculationDate,
    changeReason = changeReason,
    provisional = null,
    version = TierVersion.V2,
  )

  private fun getRiskTierOrNull(crn: String): uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier? = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
    is ClientResult.Success -> tierResponse.body
    is ClientResult.Failure -> null
  }

  private fun getCaseSummary(crn: String): CaseSummary = when (
    val caseSummariesResponse = apDeliusContextApiClient.getCaseSummaries(listOf(crn))
  ) {
    is ClientResult.Success ->
      caseSummariesResponse.body.cases.firstOrNull() ?: throw NotFoundProblem(crn, "Offender")

    is ClientResult.Failure -> caseSummariesResponse.throwException()
  }
}
