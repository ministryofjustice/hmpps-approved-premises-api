package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.CaseEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class CaseEntityFactory : Factory<CaseEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(7) }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var tierV2: Yielded<Tier?> = { TierFactory().produce() }
  private var tierV3: Yielded<Tier?> = { TierFactory().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(randomInt(0, 365).toLong()) }
  private var lastUpdatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var version: Yielded<Long> = { 1L }

  fun withId(id: UUID) = apply { this.id = { id } }
  fun withCrn(crn: String) = apply { this.crn = { crn } }
  fun withNomsNumber(nomsNumber: String?) = apply { this.nomsNumber = { nomsNumber } }
  fun withName(name: String) = apply { this.name = { name } }
  fun withTierV2(tierV2: Tier?) = apply { this.tierV2 = { tierV2 } }
  fun withTierV3(tierV3: Tier?) = apply { this.tierV3 = { tierV3 } }
  fun withCreatedAt(createdAt: OffsetDateTime) = apply { this.createdAt = { createdAt } }
  fun withLastUpdatedAt(lastUpdatedAt: OffsetDateTime) = apply { this.lastUpdatedAt = { lastUpdatedAt } }
  fun withVersion(version: Long) = apply { this.version = { version } }

  override fun produce(): CaseEntity = CaseEntity(
    id = this.id(),
    crn = this.crn(),
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    tierV2 = this.tierV2(),
    tierV3 = this.tierV3(),
    createdAt = this.createdAt(),
    lastUpdatedAt = this.lastUpdatedAt(),
    version = this.version(),
  )
}
