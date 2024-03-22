package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class ProbationAreaProbationRegionMappingEntityFactory : Factory<ProbationAreaProbationRegionMappingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var probationAreaDeliusCode: Yielded<String> = { randomStringUpperCase(6) }
  private var probationRegion: Yielded<ProbationRegionEntity>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withProbationAreaDeliusCode(probationAreaDeliusCode: String) = apply {
    this.probationAreaDeliusCode = { probationAreaDeliusCode }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withDefaultProbationRegion() = apply {
    this.probationRegion = {
      ProbationRegionEntityFactory()
        .withDefaults()
        .produce()
    }
  }

  override fun produce() = ProbationAreaProbationRegionMappingEntity(
    id = this.id(),
    probationAreaDeliusCode = this.probationAreaDeliusCode(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a Probation Region"),
  )
}
