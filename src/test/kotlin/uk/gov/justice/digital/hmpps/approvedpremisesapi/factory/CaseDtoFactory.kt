package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.CaseDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime

class CaseDtoFactory : Factory<CaseDto> {
  private var crn: Yielded<String> = { randomStringUpperCase(7) }
  private var nomsNumber: Yielded<String?> = { randomStringUpperCase(6) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var tier: Yielded<TierDto?> = { TierDtoFactory().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().minusDays(randomInt(0, 365).toLong()) }
  private var lastUpdatedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }

  fun withCrn(crn: String) = apply { this.crn = { crn } }
  fun withNomsNumber(nomsNumber: String?) = apply { this.nomsNumber = { nomsNumber } }
  fun withName(name: String) = apply { this.name = { name } }
  fun withTier(tier: TierDto?) = apply { this.tier = { tier } }
  fun withCreatedAt(createdAt: OffsetDateTime) = apply { this.createdAt = { createdAt } }
  fun withLastUpdatedAt(lastUpdatedAt: OffsetDateTime) = apply { this.lastUpdatedAt = { lastUpdatedAt } }

  override fun produce(): CaseDto = CaseDto(
    crn = this.crn(),
    nomsNumber = this.nomsNumber(),
    name = this.name(),
    tier = this.tier(),
    createdAt = this.createdAt(),
    lastUpdatedAt = this.lastUpdatedAt(),
  )
}
