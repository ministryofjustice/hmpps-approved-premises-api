package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas3LostBedReasonEntityFactory : Factory<Cas3LostBedReasonEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var isActive: Yielded<Boolean> = { true }
  private var serviceScope: Yielded<String> = { randomStringUpperCase(4) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withServiceScope(serviceScope: String) = apply {
    this.serviceScope = { serviceScope }
  }

  override fun produce(): Cas3LostBedReasonEntity = Cas3LostBedReasonEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive(),
    serviceScope = this.serviceScope(),
  )
}
