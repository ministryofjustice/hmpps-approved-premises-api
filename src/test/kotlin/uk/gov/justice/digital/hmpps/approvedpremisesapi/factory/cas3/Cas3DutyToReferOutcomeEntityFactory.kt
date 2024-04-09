package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas3DutyToReferOutcomeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class Cas3DutyToReferOutcomeEntityFactory : Factory<Cas3DutyToReferOutcomeEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var isActive: Yielded<Boolean> = { true }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIsActive(isActive: Boolean) = apply {
    this.isActive = { isActive }
  }

  override fun produce(): Cas3DutyToReferOutcomeEntity = Cas3DutyToReferOutcomeEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive(),
  )
}
