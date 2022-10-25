package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.assessrisksandneeds

import java.time.LocalDateTime

data class Needs(
  val identifiedNeeds: List<Need>,
  val notIdentifiedNeeds: List<Need>,
  val unansweredNeeds: List<Need>,
  val assessedOn: LocalDateTime
)

data class Need(
  val section: String? = null,
  val name: String? = null,
  val overThreshold: Boolean? = null,
  val riskOfHarm: Boolean? = null,
  val riskOfReoffending: Boolean? = null,
  val flaggedAsNeed: Boolean? = null,
  val severity: NeedSeverity? = null,
  val identifiedAsNeed: Boolean? = null,
  val needScore: Long? = null
)

enum class NeedSeverity {
  NO_NEED,
  STANDARD,
  SEVERE
}
