package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.entity.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.util.UUID

/**
 * JSONB representation stored in cases.tier_v2 and cases.tier_v3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Tier(
  val tierScore: String,
  val calculationId: UUID,
  val calculationDate: LocalDateTime,
  val changeReason: String?,
  val provisional: Boolean? = null,
  val version: TierVersion,
)

enum class TierVersion {
  V2,
  V3,
}
