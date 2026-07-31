package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas1CharacteristicEntityFactory : Factory<Cas1CharacteristicEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(10) }
  private var propertyName: Yielded<String> = { randomStringUpperCase(7) }
  private var modelScope: Yielded<String> = { Characteristic.ModelScope.values().map { it.value }.random() }
  private var isActive: Yielded<Boolean> = { true }

  fun withId(id: UUID) = apply {
    this.id = { id }
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

  override fun produce(): Cas1CharacteristicEntity = Cas1CharacteristicEntity(
    id = this.id(),
    propertyName = this.propertyName(),
    name = this.name(),
    modelScope = this.modelScope(),
    isActive = this.isActive(),
  )
}
