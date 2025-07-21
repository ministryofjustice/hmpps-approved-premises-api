package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PremisesLocalRestrictionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class Cas1PremisesLocalRestrictionEntityFactory : Factory<Cas1PremisesLocalRestrictionEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var description: Yielded<String> = { randomStringUpperCase(10) }
  private var createdByUserId: Yielded<UUID> = { UUID.randomUUID() }
  private var approvedPremisesId: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var archived: Yielded<Boolean> = { false }

  fun withId(id: UUID) = apply { this.id = { id } }
  fun withDescription(description: String) = apply { this.description = { description } }
  fun withCreatedByUserId(createdByUserId: UUID) = apply { this.createdByUserId = { createdByUserId } }
  fun withApprovedPremisesId(approvedPremisesId: UUID) = apply { this.approvedPremisesId = { approvedPremisesId } }
  fun withCreatedAt(createdAt: OffsetDateTime) = apply { this.createdAt = { createdAt } }
  fun withArchived(archived: Boolean) = apply { this.archived = { archived } }

  override fun produce(): Cas1PremisesLocalRestrictionEntity = Cas1PremisesLocalRestrictionEntity(
    id = this.id(),
    description = this.description(),
    createdAt = this.createdAt(),
    createdByUserId = this.createdByUserId(),
    approvedPremisesId = this.approvedPremisesId(),
    archived = this.archived(),
  )
}
