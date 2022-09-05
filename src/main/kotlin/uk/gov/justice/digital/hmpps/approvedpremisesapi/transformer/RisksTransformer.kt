package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks

@Component
class RisksTransformer {
  fun transformDomainToApi(domain: PersonRisks) = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks(
    crn = domain.crn,
    roshRisks = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RoshRisks(
      overallRisk = domain.roshRisks.overallRisk,
      riskToChildren = domain.roshRisks.riskToChildren,
      riskToPublic = domain.roshRisks.riskToPublic,
      riskToKnownAdult = domain.roshRisks.riskToKnownAdult,
      riskToStaff = domain.roshRisks.riskToStaff,
      lastUpdated = domain.roshRisks.lastUpdated
    ),
    mappa = domain.mappa?.let {
      uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Mappa(
        level = domain.mappa.level,
        lastUpdated = domain.mappa.lastUpdated
      )
    },
    tier = uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTier(
      level = domain.tier.level,
      lastUpdated = domain.tier.lastUpdated
    )
  )
}
