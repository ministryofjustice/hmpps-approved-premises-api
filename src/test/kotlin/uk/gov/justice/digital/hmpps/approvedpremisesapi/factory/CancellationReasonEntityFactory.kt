package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class CancellationReasonEntityFactory(
  cancellationReasonTestRepository: CancellationReasonTestRepository
) : PersistedFactory<CancellationReasonEntity, UUID>(cancellationReasonTestRepository) {
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

  override fun produce(): CancellationReasonEntity = CancellationReasonEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive()
  )
}
