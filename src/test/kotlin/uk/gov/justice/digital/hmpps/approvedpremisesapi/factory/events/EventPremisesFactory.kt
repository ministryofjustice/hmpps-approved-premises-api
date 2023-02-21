package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class EventPremisesFactory : Factory<Premises> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var apCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var legacyApCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var probationArea: Yielded<ProbationArea> = { ProbationAreaFactory().produce() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withLegacyApCode(legacyApCode: String) = apply {
    this.legacyApCode = { legacyApCode }
  }

  fun withProbationArea(probationArea: ProbationArea) = apply {
    this.probationArea = { probationArea }
  }

  override fun produce() = Premises(
    id = this.id(),
    name = this.name(),
    apCode = this.apCode(),
    legacyApCode = this.legacyApCode(),
    probationArea = this.probationArea()
  )
}
