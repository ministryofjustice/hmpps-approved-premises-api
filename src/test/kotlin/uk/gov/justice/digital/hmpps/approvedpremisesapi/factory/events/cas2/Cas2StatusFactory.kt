package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.StatusDetailItem

class Cas2StatusFactory : Factory<Cas2Status> {
  private var name: Yielded<String> = { "moreInfoRequested" }
  private var label: Yielded<String> = { "More information requested" }
  private var description: Yielded<String> = { "More information about the application has been requested" }
  private var statusDetails = emptyList<StatusDetailItem>()

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withStatusDetails(statusDetails: List<StatusDetailItem>) = apply {
    this.statusDetails = statusDetails
  }

  override fun produce(): Cas2Status = Cas2Status(
    name = this.name(),
    label = this.label(),
    description = this.description(),
    statusDetails = this.statusDetails
  )
}
