package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import java.time.LocalDateTime

data class EventTier(
  val tierScore: String,
  val calculationDate: LocalDateTime,
  val provisional: Boolean?,
  val version: EventTierVersion,
)

enum class EventTierVersion {
  V2,
  V3,
}
