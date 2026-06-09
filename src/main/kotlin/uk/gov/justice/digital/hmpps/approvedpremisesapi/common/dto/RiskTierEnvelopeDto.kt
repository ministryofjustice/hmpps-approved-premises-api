package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "RiskTierEnvelope")
data class RiskTierEnvelopeDto(
  val status: RiskEnvelopeStatusDto,
  val `value`: RiskTierDto? = null,
)
