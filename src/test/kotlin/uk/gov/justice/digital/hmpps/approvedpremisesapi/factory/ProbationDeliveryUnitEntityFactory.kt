package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class ProbationDeliveryUnitEntityFactory : Factory<ProbationDeliveryUnitEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var deliusCode: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var probationRegion: Yielded<ProbationRegionEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withDeliusCode(deliusCode: String) = apply {
    this.deliusCode = { deliusCode }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  override fun produce(): ProbationDeliveryUnitEntity = ProbationDeliveryUnitEntity(
    id = this.id(),
    name = this.name(),
    deliusCode = this.deliusCode(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a Probation Region"),
  )
}
