package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventNamedId
import java.util.UUID

class Cas1DomainEventNamedIdFactory : Factory<Cas1DomainEventNamedId> {
  override fun produce(): Cas1DomainEventNamedId = Cas1DomainEventNamedId(
    id = UUID.randomUUID(),
    name = "thename",
  )
}
