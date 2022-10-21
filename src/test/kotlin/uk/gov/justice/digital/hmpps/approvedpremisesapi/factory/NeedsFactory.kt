package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Need
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds.Needs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDateTime

class NeedsFactory : Factory<Needs> {
  private var identifiedNeeds: Yielded<List<Need>> = { listOf() }
  private var notIdentifiedNeeds: Yielded<List<Need>> = { listOf() }
  private var unansweredNeeds: Yielded<List<Need>> = { listOf() }
  private var assessedOn: Yielded<LocalDateTime> = { LocalDateTime.now().randomDateTimeBefore(7) }

  fun withIdentifiedNeeds(needs: List<Need>) = apply {
    this.identifiedNeeds = { needs }
  }

  fun withNotIdentifiedNeeds(notIdentifiedNeeds: List<Need>) = apply {
    this.notIdentifiedNeeds = { notIdentifiedNeeds }
  }

  fun withUnansweredNeeds(unansweredNeeds: List<Need>) = apply {
    this.unansweredNeeds = { unansweredNeeds }
  }

  fun withAssessedOn(assessedOn: LocalDateTime) = apply {
    this.assessedOn = { assessedOn }
  }

  override fun produce(): Needs = Needs(
    identifiedNeeds = this.identifiedNeeds(),
    notIdentifiedNeeds = this.notIdentifiedNeeds(),
    unansweredNeeds = this.unansweredNeeds(),
    assessedOn = this.assessedOn()
  )
}
