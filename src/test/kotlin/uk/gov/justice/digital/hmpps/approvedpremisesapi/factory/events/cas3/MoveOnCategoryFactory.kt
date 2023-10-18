package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class MoveOnCategoryFactory : Factory<MoveOnCategory> {
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var label: Yielded<String> = { randomStringMultiCaseWithNumbers(12) }

  fun withDescriptiion(description: String) = apply {
    this.description = { description }
  }

  fun withLabel(label: String) = apply {
    this.label = { label }
  }

  override fun produce() = MoveOnCategory(
    description = this.description(),
    label = this.label(),
  )
}
