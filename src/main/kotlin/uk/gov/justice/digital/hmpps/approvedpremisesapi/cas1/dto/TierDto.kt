package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class TierDto(
  val tierScore: String,
  @Schema(description = "The date that the tier score last changed, or the most recent calculation date if this is the first time we've captured the tier for the given CRN")
  val calculationDate: LocalDateTime,
  @Schema(description = "Provisional will only be provided when version is V3")
  val provisional: Boolean? = null,
  val version: TierVersionDto,
)

enum class TierVersionDto {
  V2,
  V3,
}
