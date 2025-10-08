package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesCharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas3PremisesCharacteristicEntityFactory : Factory<Cas3PremisesCharacteristicEntity> {

  private var id: UUID = UUID.randomUUID()
  private var name: String = randomStringUpperCase(10)
  private var description: String = randomStringUpperCase(7)
  private var isActive: Boolean = true

  fun id(id: UUID) = apply {
    this.id = id
  }

  fun name(name: String) = apply {
    this.name = name
  }

  fun description(description: String) = apply {
    this.description = description
  }

  fun isActive(isActive: Boolean) = apply {
    this.isActive = isActive
  }

  override fun produce(): Cas3PremisesCharacteristicEntity = Cas3PremisesCharacteristicEntity(
    id = this.id,
    name = this.name,
    description = this.description,
    active = this.isActive,
  )
}
