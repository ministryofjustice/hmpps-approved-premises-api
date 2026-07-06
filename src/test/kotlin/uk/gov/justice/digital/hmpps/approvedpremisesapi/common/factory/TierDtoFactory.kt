package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.LocalDateTime

class TierDtoFactory : Factory<TierDto> {
  private var tierScore: Yielded<String> = { randomStringLowerCase(8) }
  private var calculationDate: Yielded<LocalDateTime> = { LocalDateTime.now() }
  private var provisional: Yielded<Boolean?> = { null }
  private var version: Yielded<TierVersionDto> = { TierVersionDto.V2 }

  fun withVersion(version: TierVersionDto) = apply {
    this.version = { version }
  }

  override fun produce(): TierDto = TierDto(
    tierScore = this.tierScore(),
    calculationDate = this.calculationDate(),
    provisional = this.provisional(),
    version = this.version(),
  )
}
