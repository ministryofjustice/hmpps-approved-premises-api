package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class DepartureReasonEntityFactory : Factory<DepartureReasonEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var isActive: Yielded<Boolean> = { true }
  private var serviceScope: Yielded<String> = { randomStringUpperCase(4) }
  private var legacyDeliusCategoryCode: Yielded<String> = { randomStringUpperCase(1) }
  private var parentReasonId: Yielded<DepartureReasonEntity?> = { null }

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

  fun withLegacyDeliusCategoryCode(legacyDeliusCategoryCode: String) = apply {
    this.legacyDeliusCategoryCode = { legacyDeliusCategoryCode }
  }

  fun withParentReasonId(parentDepartureReason: DepartureReasonEntity) = apply {
    this.parentReasonId = { parentDepartureReason }
  }

  override fun produce(): DepartureReasonEntity = DepartureReasonEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive(),
    serviceScope = this.serviceScope(),
    legacyDeliusReasonCode = this.legacyDeliusCategoryCode(),
    parentReasonId = this.parentReasonId(),
  )
}
