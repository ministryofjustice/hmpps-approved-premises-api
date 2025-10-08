package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspaceCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas3BedspaceCharacteristicEntityFactory : Factory<Cas3BedspaceCharacteristicEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var description: Yielded<String> = { randomStringUpperCase(10) }
  private var name: Yielded<String> = { randomStringUpperCase(7) }
  private var isActive: Yielded<Boolean> = { true }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  override fun produce(): Cas3BedspaceCharacteristicEntity = Cas3BedspaceCharacteristicEntity(
    id = this.id(),
    description = this.description(),
    name = this.name(),
    active = this.isActive(),
  )
}
