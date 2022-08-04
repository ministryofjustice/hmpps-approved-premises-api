package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class ApAreaEntityFactory(
  apAreaTestRepository: ApAreaTestRepository
) : PersistedFactory<ApAreaEntity, UUID>(apAreaTestRepository) {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce(): ApAreaEntity = ApAreaEntity(
    id = this.id(),
    name = this.name(),
    premises = mutableListOf()
  )
}
