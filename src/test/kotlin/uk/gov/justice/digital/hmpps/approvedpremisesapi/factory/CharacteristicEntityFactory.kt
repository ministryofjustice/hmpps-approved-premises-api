package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class CharacteristicEntityFactory : Factory<CharacteristicEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(10) }
  private var serviceScope: Yielded<String> = { randomStringUpperCase(4) }
  private var modelScope: Yielded<String> = { randomStringUpperCase(4) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withServiceScope(serviceScope: String) = apply {
    this.serviceScope = { serviceScope }
  }

  fun withModelScope(modelScope: String) = apply {
    this.modelScope = { modelScope }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce(): CharacteristicEntity = CharacteristicEntity(
    id = this.id(),
    name = this.name(),
    serviceScope = this.serviceScope(),
    modelScope = this.modelScope()
  )
}
