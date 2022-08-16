package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class ProbationRegionEntityFactory : Factory<ProbationRegionEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var apArea: Yielded<ApAreaEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withYieldedApArea(apArea: Yielded<ApAreaEntity>) = apply {
    this.apArea = apArea
  }

  fun withApArea(apArea: ApAreaEntity) = apply {
    this.apArea = { apArea }
  }

  override fun produce(): ProbationRegionEntity = ProbationRegionEntity(
    id = this.id(),
    name = this.name(),
    premises = mutableListOf(),
    apArea = this.apArea?.invoke() ?: throw RuntimeException("Must provide an ApArea")
  )
}
