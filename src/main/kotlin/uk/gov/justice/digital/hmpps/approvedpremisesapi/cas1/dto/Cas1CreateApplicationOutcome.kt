package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import java.util.UUID

data class Cas1CreateApplicationOutcome(
  val applicationId: UUID? = null,
  val tier: TierDto? = null,
)
