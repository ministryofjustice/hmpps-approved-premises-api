package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks

class PersonRisksFactory : Factory<PersonRisks> {
  private var roshRisks: Yielded<RiskWithStatus<RoshRisks>> = { RiskWithStatus(status = RiskStatus.NotFound) }
  private var mappa: Yielded<RiskWithStatus<Mappa>> = { RiskWithStatus(status = RiskStatus.NotFound) }
  private var tier: Yielded<RiskWithStatus<RiskTier>> = { RiskWithStatus(status = RiskStatus.NotFound) }
  private var flags: Yielded<RiskWithStatus<List<String>>> = { RiskWithStatus(status = RiskStatus.NotFound) }

  fun withRoshRisks(roshRisks: RiskWithStatus<RoshRisks>) = apply {
    this.roshRisks = { roshRisks }
  }

  fun withMappa(mappa: RiskWithStatus<Mappa>) = apply {
    this.mappa = { mappa }
  }

  fun withTier(tier: RiskWithStatus<RiskTier>) = apply {
    this.tier = { tier }
  }

  fun withFlags(flags: RiskWithStatus<List<String>>) = apply {
    this.flags = { flags }
  }

  override fun produce(): PersonRisks = PersonRisks(
    roshRisks = this.roshRisks(),
    mappa = this.mappa(),
    tier = this.tier(),
    flags = this.flags(),
  )
}
