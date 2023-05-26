package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Conviction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Offence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate

class ConvictionFactory : Factory<Conviction> {
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var index: Yielded<String> = { "1" }
  private var active: Yielded<Boolean> = { true }
  private var inBreach: Yielded<Boolean> = { false }
  private var failureToComplyCount: Yielded<Long> = { 0 }
  private var breachEnd: Yielded<LocalDate?> = { null }
  private var awaitingPsr: Yielded<Boolean> = { false }
  private var convictionDate: Yielded<LocalDate?> = { LocalDate.now().randomDateBefore(10) }
  private var referralDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(10) }
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

  fun withInBreach(inBreach: Boolean) = apply {
    this.inBreach = { inBreach }
  }

  fun withFailureToComplyCount(failureToComplyCount: Long) = apply {
    this.failureToComplyCount = { failureToComplyCount }
  }

  fun withBreachEnd(breachEnd: LocalDate?) = apply {
    this.breachEnd = { breachEnd }
  }

  fun withAwaitingPsr(awaitingPsr: Boolean) = apply {
    this.awaitingPsr = { awaitingPsr }
  }

  fun withConvictionDate(convictionDate: LocalDate?) = apply {
    this.convictionDate = { convictionDate }
  }

  fun withReferralDate(referralDate: LocalDate) = apply {
    this.referralDate = { referralDate }
  }

  fun withOffences(offences: List<Offence>) = apply {
    this.offences = { offences }
  }

  override fun produce(): Conviction = Conviction(
    convictionId = this.convictionId(),
    index = this.index(),
    active = this.active(),
    inBreach = this.inBreach(),
    failureToComplyCount = this.failureToComplyCount(),
    breachEnd = this.breachEnd(),
    awaitingPsr = this.awaitingPsr(),
    convictionDate = this.convictionDate(),
    referralDate = this.referralDate(),
    offences = this.offences(),
  )
}
