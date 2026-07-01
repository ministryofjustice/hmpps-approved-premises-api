package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import java.time.LocalDateTime

data class Cas1TierDto(
  val tierScore: String,
  val calculationDate: LocalDateTime,
  val provisional: Boolean? = null,
  val version: Cas1TierVersionDto,
)

enum class Cas1TierVersionDto {
  V2,
  V3,
}
