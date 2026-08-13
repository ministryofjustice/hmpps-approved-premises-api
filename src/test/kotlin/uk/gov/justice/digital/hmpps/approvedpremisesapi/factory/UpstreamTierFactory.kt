package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.LocalDateTime
import java.util.UUID

class UpstreamTierFactory : Factory<Tier> {
  private var tierScore: Yielded<String> = { randomStringLowerCase(8) }
  private var calculationId: Yielded<UUID> = { UUID.randomUUID() }
  private var calculationDate: Yielded<LocalDateTime> = { LocalDateTime.now() }
  private var changeReason: Yielded<String?> = { null }
  private var provisional: Yielded<Boolean?> = { null }

  fun withTierScore(tierScore: String) = apply {
    this.tierScore = { tierScore }
  }

  fun withCalculationId(calculationId: UUID) = apply {
    this.calculationId = { calculationId }
  }

  fun withCalculationDate(calculationDate: LocalDateTime) = apply {
    this.calculationDate = { calculationDate }
  }

  fun withChangeReason(changeReason: String?) = apply {
    this.changeReason = { changeReason }
  }

  fun withProvisional(provisional: Boolean?) = apply {
    this.provisional = { provisional }
  }

  override fun produce(): Tier = Tier(
    tierScore = this.tierScore(),
    calculationId = this.calculationId(),
    calculationDate = this.calculationDate(),
    changeReason = this.changeReason(),
    provisional = this.provisional(),
  )
}
