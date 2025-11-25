package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.time.Instant
import java.util.*

data class Cas1ReferralHistory(
  val type: ServiceType,
  val id: UUID,
  val applicationId: UUID,
  val status: Cas1AssessmentStatus,
  val createdAt: Instant,
)
