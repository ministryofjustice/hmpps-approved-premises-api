package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.time.OffsetDateTime
import java.util.UUID

data class Cas2ReferralHistory(
  val type: String,
  val id: UUID,
  val applicationId: UUID,
  val status: String,
  val createdAt: OffsetDateTime,
)
