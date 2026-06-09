package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(name = "RiskTier")
data class RiskTierDto(
  val level: String,
  val lastUpdated: LocalDate,
)
