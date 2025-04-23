package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventCodedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas1DomainEventCodedIdFactory : Factory<Cas1DomainEventCodedId> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var code: Yielded<String> = { randomStringUpperCase(6) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCode(code: String) = apply {
    this.code = { code }
  }

  override fun produce(): Cas1DomainEventCodedId = Cas1DomainEventCodedId(
    id = id(),
    code = code(),
  )
}
