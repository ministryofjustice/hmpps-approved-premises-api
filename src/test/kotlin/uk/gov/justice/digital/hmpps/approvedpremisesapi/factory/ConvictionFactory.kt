package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Offence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt

class ConvictionFactory : Factory<Conviction> {
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var index: Yielded<String> = { "1" }
  private var active: Yielded<Boolean> = { true }
  private var offences: Yielded<List<Offence>> = { listOf() }

  fun withConvictionId(convictionId: Long) = apply {
    this.convictionId = { convictionId }
  }

  fun withIndex(index: String) = apply {
    this.index = { index }
  }

  fun withActive(active: Boolean) = apply {
    this.active = { active }
  }

  fun withOffences(offences: List<Offence>) = apply {
    this.offences = { offences }
  }

  override fun produce(): Conviction = Conviction(
    convictionId = this.convictionId(),
    index = this.index(),
    active = this.active(),
    offences = this.offences(),
  )
}
