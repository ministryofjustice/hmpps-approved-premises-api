package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import java.time.LocalDateTime

data class TierDto(
  val tierScore: String,
  val calculationDate: LocalDateTime,
  val provisional: Boolean? = null,
  val version: TierVersionDto,
)

enum class TierVersionDto {
  V2,
  V3,
}
