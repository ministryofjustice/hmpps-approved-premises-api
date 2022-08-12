package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.KeyWorkerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.KeyWorkerTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class KeyWorkerEntityFactory(
  keyWorkerTestRepository: KeyWorkerTestRepository
) : PersistedFactory<KeyWorkerEntity, UUID>(keyWorkerTestRepository) {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringUpperCase(12) }
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

  override fun produce(): KeyWorkerEntity = KeyWorkerEntity(
    id = this.id(),
    name = this.name(),
    isActive = this.isActive(),
    bookings = mutableListOf()
  )
}
