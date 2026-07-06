package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption

data class Cas1RequestsForPlacementDurationsCalculationRequestDto(
  val apType: ApType,
  val tier: TierDto,
  val isWomensApplication: Boolean,
  val sentenceType: SentenceTypeOption,
)
