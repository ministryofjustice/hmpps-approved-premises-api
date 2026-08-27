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
  /**
   * The date that the tier score last changed, or the most recent
   * calculation date if this is the first time we've captured the
   * tier for the given CRN
   */
  val calculationDate: LocalDateTime,
  val changeReason: String?,
  /**
   * Provisional will only be provided when version is V3
   */
  val provisional: Boolean? = null,
  val version: TierVersion,
)

enum class TierVersion {
  V2,
  V3,
}

enum class TierV3Score {
  A,
  B,
  C,
  D,
  E,
  F,
  G,
  MISSING,
  NOT_SUPERVISED,
}
