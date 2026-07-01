package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.LocalDateTime

class Cas1TierDtoFactory : Factory<Cas1TierDto> {
  private var tierScore: Yielded<String> = { randomStringLowerCase(8) }
  private var calculationDate: Yielded<LocalDateTime> = { LocalDateTime.now() }
  private var provisional: Yielded<Boolean?> = { null }
  private var version: Yielded<Cas1TierVersionDto> = { Cas1TierVersionDto.V2 }

  fun withVersion(version: Cas1TierVersionDto) = apply {
    this.version = { version }
  }

  override fun produce(): Cas1TierDto = Cas1TierDto(
    tierScore = this.tierScore(),
    calculationDate = this.calculationDate(),
    provisional = this.provisional(),
    version = this.version(),
  )
}
