package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestsForPlacementDurationsCalculationRequestDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory.TierDtoFactory

class Cas1RequestsForPlacementDurationsCalculationRequestDtoFactory : Factory<Cas1RequestsForPlacementDurationsCalculationRequestDto> {
  private var apType: Yielded<ApType> = { ApType.normal }
  private var tier: Yielded<TierDto> = { TierDtoFactory().produce() }
  private var isWomensApplication: Yielded<Boolean> = { false }
  private var sentenceType: Yielded<SentenceTypeOption> = { SentenceTypeOption.standardDeterminate }

  fun withApType(apType: ApType) = apply {
    this.apType = { apType }
  }

  fun withTier(tier: TierDto) = apply {
    this.tier = { tier }
  }

  override fun produce(): Cas1RequestsForPlacementDurationsCalculationRequestDto = Cas1RequestsForPlacementDurationsCalculationRequestDto(
    apType = this.apType(),
    tier = this.tier(),
    isWomensApplication = this.isWomensApplication(),
    sentenceType = this.sentenceType(),
  )
}
