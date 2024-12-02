package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks

@Component
class RisksTransformer {
  fun transformDomainToApi(domain: PersonRisks, crn: String) = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks(
    crn = crn,
    roshRisks = transformRoshDomainToApi(domain.roshRisks),
    mappa = transformMappaDomainToApi(domain.mappa),
    tier = transformTierDomainToApi(domain.tier),
    flags = transformFlagsDomainToApi(domain.flags),
  )

  fun transformRoshDomainToApi(domain: RiskWithStatus<RoshRisks>) = RoshRisksEnvelope(
    status = transformStatusDomainToApi(domain.status),
    value = domain.value?.let {
      uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks(
        overallRisk = it.overallRisk,
        riskToChildren = it.riskToChildren,
        riskToPublic = it.riskToPublic,
        riskToKnownAdult = it.riskToKnownAdult,
        riskToStaff = it.riskToStaff,
        lastUpdated = it.lastUpdated,
      )
    },
  )

  fun transformMappaDomainToApi(domain: RiskWithStatus<Mappa>) = MappaEnvelope(
    status = transformStatusDomainToApi(domain.status),
    value = domain.value?.let {
      uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa(
        level = it.level,
        lastUpdated = it.lastUpdated,
      )
    },
  )

  fun transformTierDomainToApi(domain: RiskWithStatus<RiskTier>) = RiskTierEnvelope(
    status = transformStatusDomainToApi(domain.status),
    value = domain.value?.let {
      uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTier(
        level = it.level,
        lastUpdated = it.lastUpdated,
      )
    },
  )

  private fun transformFlagsDomainToApi(domain: RiskWithStatus<List<String>>) = FlagsEnvelope(
    status = transformStatusDomainToApi(domain.status),
    value = domain.value,
  )

  private fun transformStatusDomainToApi(domain: RiskStatus) = when (domain) {
    RiskStatus.Retrieved -> RiskEnvelopeStatus.RETRIEVED
    RiskStatus.NotFound -> RiskEnvelopeStatus.NOT_FOUND
    RiskStatus.Error -> RiskEnvelopeStatus.ERROR
  }
}
