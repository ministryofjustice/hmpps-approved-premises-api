package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FlagsEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MappaEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskEnvelopeStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisksEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus

@Component
class RisksTransformer {
  fun transformDomainToApi(domain: PersonRisks, crn: String) = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks(
    crn = crn,
    roshRisks = RoshRisksEnvelope(
      status = transformDomainToApi(domain.roshRisks.status),
      value = domain.roshRisks.value?.let {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks(
          overallRisk = it.overallRisk,
          riskToChildren = it.riskToChildren,
          riskToPublic = it.riskToPublic,
          riskToKnownAdult = it.riskToKnownAdult,
          riskToStaff = it.riskToStaff,
          lastUpdated = it.lastUpdated
        )
      }
    ),
    mappa = MappaEnvelope(
      status = transformDomainToApi(domain.mappa.status),
      value = domain.mappa.value?.let {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa(
          level = it.level,
          lastUpdated = it.lastUpdated
        )
      }
    ),
    tier = RiskTierEnvelope(
      status = transformDomainToApi(domain.tier.status),
      value = domain.tier.value?.let {
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTier(
          level = it.level,
          lastUpdated = it.lastUpdated
        )
      }
    ),
    flags = FlagsEnvelope(
      status = transformDomainToApi(domain.flags.status),
      value = domain.flags.value
    )
  )

  private fun transformDomainToApi(domain: RiskStatus) = when (domain) {
    RiskStatus.Retrieved -> RiskEnvelopeStatus.retrieved
    RiskStatus.NotFound -> RiskEnvelopeStatus.notFound
    RiskStatus.Error -> RiskEnvelopeStatus.error
  }
}
