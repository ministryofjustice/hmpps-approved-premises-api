package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class NonArrivalReasonEntityFactory : Factory<NonArrivalReasonEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var isActive: Yielded<Boolean> = { true }
  private var legacyDeliusReasonCode: Yielded<String> = { randomOf(listOf("A", "B", "C", "D", "1H", "4I")) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  fun withLegacyDeliusReasonCode(legacyDeliusReasonCode: String) = apply {
    this.legacyDeliusReasonCode = { legacyDeliusReasonCode }
  }

  override fun produce(): NonArrivalReasonEntity = NonArrivalReasonEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive(),
    legacyDeliusReasonCode = this.legacyDeliusReasonCode(),
  )
}
