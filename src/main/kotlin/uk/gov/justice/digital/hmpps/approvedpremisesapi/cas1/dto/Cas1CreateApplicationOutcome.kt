package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class Cas1CreateApplicationOutcome(
  val outcome: TierEligibility,
  val applicationId: UUID? = null,
  val tier: TierDto? = null,
)

enum class TierEligibility(@JsonValue val value: String) {
  ELIGIBLE("eligible"),
  NOT_ELIGIBLE("not_eligible"),
  NOT_FOUND("not_found"),
}
