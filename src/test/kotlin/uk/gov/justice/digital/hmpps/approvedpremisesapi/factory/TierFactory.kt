package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.TierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.LocalDateTime
import java.util.UUID

class TierFactory : Factory<Tier> {
  private var tierScore: Yielded<String> = { randomStringLowerCase(8) }
  private var calculationDate: Yielded<LocalDateTime> = { LocalDateTime.now() }
  private var calculationId: Yielded<UUID> = { UUID.randomUUID() }
  private var provisional: Yielded<Boolean?> = { null }
  private var version: Yielded<TierVersion> = { TierVersion.V2 }
  private var changeReason: Yielded<String?> = { null }

  fun withVersion(version: TierVersion) = apply {
    this.version = { version }
  }

  fun withTierScore(tierScore: String) = apply {
    this.tierScore = { tierScore }
  }

  fun withChangeReason(changeReason: String) = apply {
    this.changeReason = { changeReason }
  }

  fun withCalculationDate(calculationDate: LocalDateTime) = apply {
    this.calculationDate = { calculationDate }
  }
  fun withCalculationId(calculationId: UUID) = apply {
    this.calculationId = { calculationId }
  }
  fun withProvisional(provisional: Boolean) = apply {
    this.provisional = { provisional }
  }

  override fun produce(): Tier = Tier(
    tierScore = this.tierScore(),
    calculationDate = this.calculationDate(),
    provisional = this.provisional(),
    version = this.version(),
    calculationId = this.calculationId(),
    changeReason = this.changeReason(),
  )
}
