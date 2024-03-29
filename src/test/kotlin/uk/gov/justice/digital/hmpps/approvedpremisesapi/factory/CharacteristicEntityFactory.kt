package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class CharacteristicEntityFactory : Factory<CharacteristicEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(10) }
  private var propertyName: Yielded<String> = { randomStringUpperCase(7) }
  private var serviceScope: Yielded<String> = { Characteristic.ServiceScope.values().map { it.value }.random() }
  private var modelScope: Yielded<String> = { Characteristic.ModelScope.values().map { it.value }.random() }
  private var isActive: Yielded<Boolean> = { true }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withServiceScope(serviceScope: String) = apply {
    this.serviceScope = { serviceScope }
  }

  fun withModelScope(modelScope: String) = apply {
    this.modelScope = { modelScope }
  }

  fun withPropertyName(propertyName: String) = apply {
    this.propertyName = { propertyName }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  override fun produce(): CharacteristicEntity = CharacteristicEntity(
    id = this.id(),
    propertyName = this.propertyName(),
    name = this.name(),
    serviceScope = this.serviceScope(),
    modelScope = this.modelScope(),
    isActive = this.isActive(),
  )
}
