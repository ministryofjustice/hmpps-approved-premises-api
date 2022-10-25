package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Need
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.NeedSeverity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class NeedFactory : Factory<Need> {
  private var section: Yielded<String> = { randomStringUpperCase(5) }
  private var name: Yielded<String?> = { randomStringUpperCase(6) }
  private var overThreshold: Yielded<Boolean?> = { null }
  private var riskOfHarm: Yielded<Boolean?> = { null }
  private var riskOfReoffending: Yielded<Boolean?> = { null }
  private var flaggedAsNeed: Yielded<Boolean?> = { null }
  private var severity: Yielded<NeedSeverity?> = { null }
  private var identifiedAsNeed: Yielded<Boolean?> = { null }
  private var needScore: Yielded<Long?> = { null }

  fun withSection(section: String) = apply {
    this.section = { section }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withOverThreshold(overThreshold: Boolean?) = apply {
    this.overThreshold = { overThreshold }
  }

  fun withRiskOfHarm(riskOfHarm: Boolean?) = apply {
    this.riskOfHarm = { riskOfHarm }
  }

  fun withRiskOfReoffending(riskOfReoffending: Boolean?) = apply {
    this.riskOfReoffending = { riskOfReoffending }
  }

  fun withFlaggedAsNeed(flaggedAsNeed: Boolean?) = apply {
    this.flaggedAsNeed = { flaggedAsNeed }
  }

  fun withSeverity(severity: NeedSeverity?) = apply {
    this.severity = { severity }
  }

  fun withIdentifiedAsNeed(identifiedAsNeed: Boolean?) = apply {
    this.identifiedAsNeed = { identifiedAsNeed }
  }

  fun withNeedScore(needScore: Long?) = apply {
    this.needScore = { needScore }
  }

  override fun produce(): Need = Need(
    section = this.section(),
    name = this.name(),
    overThreshold = this.overThreshold(),
    riskOfHarm = this.riskOfHarm(),
    riskOfReoffending = this.riskOfReoffending(),
    flaggedAsNeed = this.flaggedAsNeed(),
    severity = this.severity(),
    identifiedAsNeed = this.identifiedAsNeed(),
    needScore = this.needScore()
  )
}
