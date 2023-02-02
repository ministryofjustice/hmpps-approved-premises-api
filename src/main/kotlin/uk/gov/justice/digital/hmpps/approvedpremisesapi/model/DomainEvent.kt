package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.OffsetDateTime
import java.util.UUID

data class DomainEvent <T> (
  val id: UUID,
  val applicationId: UUID,
  val crn: String,
  val occurredAt: OffsetDateTime,
  val data: T
)
