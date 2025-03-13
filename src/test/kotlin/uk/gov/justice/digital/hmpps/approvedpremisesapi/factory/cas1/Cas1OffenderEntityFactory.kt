package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class Cas1OffenderEntityFactory : Factory<Cas1OffenderEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringUpperCase(7) }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var tier: Yielded<String?> = { listOf("A", "B", "C", "D").random() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(randomInt(0, 365).toLong()) }
  private var lastUpdatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var version: Yielded<Long> = { 1L }

  fun withId(id: UUID) = apply { this.id = { id } }
  fun withCrn(crn: String) = apply { this.crn = { crn } }
  fun withNomsNumber(nomsNumber: String?) = apply { this.nomsNumber = { nomsNumber } }
  fun withName(name: String) = apply { this.name = { name } }
  fun withTier(tier: String?) = apply { this.tier = { tier } }
  fun withCreatedAt(createdAt: OffsetDateTime) = apply { this.createdAt = { createdAt } }
  fun withLastUpdatedAt(lastUpdatedAt: OffsetDateTime) = apply { this.lastUpdatedAt = { lastUpdatedAt } }
  fun withVersion(version: Long) = apply { this.version = { version } }

  override fun produce(): Cas1OffenderEntity = Cas1OffenderEntity(
    id = this.id(),
    crn = this.crn(),
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    tier = this.tier(),
    createdAt = this.createdAt(),
    lastUpdatedAt = this.lastUpdatedAt(),
    version = this.version(),
  )
}
