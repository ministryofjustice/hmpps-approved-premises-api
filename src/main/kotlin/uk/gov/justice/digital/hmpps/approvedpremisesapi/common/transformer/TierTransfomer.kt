package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.transformer

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTierVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierVersionDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model.Tier

fun Tier.toDto() = TierDto(
  tierScore = tierScore,
  calculationDate = calculationDate,
  provisional = provisional,
  version = TierVersionDto.valueOf(version.name),
)

fun TierDto.toEventTier() = EventTier(
  tierScore = tierScore,
  calculationDate = calculationDate,
  provisional = provisional,
  version = when (version) {
    TierVersionDto.V2 -> EventTierVersion.V2
    TierVersionDto.V3 -> EventTierVersion.V3
  },
)
