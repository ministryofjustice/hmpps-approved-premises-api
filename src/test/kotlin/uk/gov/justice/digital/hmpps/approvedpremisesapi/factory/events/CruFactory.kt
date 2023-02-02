package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class CruFactory : Factory<Cru> {
  private var code: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce() = Cru(
    code = this.code(),
    name = this.name()
  )
}
