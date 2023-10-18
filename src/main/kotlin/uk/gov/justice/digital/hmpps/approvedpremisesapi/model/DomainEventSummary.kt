package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.OffsetDateTime

data class DomainEventSummary(
  val id: String,
  val type: DomainEventType,
  val occurredAt: OffsetDateTime,
)
