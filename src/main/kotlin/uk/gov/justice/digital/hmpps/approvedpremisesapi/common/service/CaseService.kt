package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseRepository
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
    val caseEntity = caseRepository.findByCrn(caseSummary.crn)?.apply {
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim()
      tier = riskTier
      nomsNumber = caseSummary.nomsId
    } ?: CaseEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn,
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim(),
      tier = riskTier,
      nomsNumber = caseSummary.nomsId,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = OffsetDateTime.now(),
    )
    return caseRepository.saveAndFlush(caseEntity)
  }

  fun getCase(crn: String): CaseDto? = caseRepository.findByCrn(crn)?.let {
    CaseDto(
      crn = it.crn,
      nomsNumber = it.nomsNumber,
      name = it.name,
      tier = it.tier,
      createdAt = it.createdAt,
      lastUpdatedAt = it.lastUpdatedAt,
    )
  }

  fun reviseTier(crn: String): Boolean {
    val case = caseRepository.findByCrn(crn)

    if (case == null) {
      return false
    }

    val tier = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> tierResponse.body.tierScore
      is ClientResult.Failure -> throw tierResponse.toException()
    }

    case.tier = tier
    caseRepository.save(case)

    log.info("Have updated tier for $crn to $tier")
    return true
  }

  private fun getRiskTierOrNull(crn: String): String? = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
    is ClientResult.Success -> tierResponse.body.tierScore
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
