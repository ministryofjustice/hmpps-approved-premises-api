package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.sql.Timestamp
import java.util.UUID

interface DomainEventSummary {
  val id: String
  val type: DomainEventType
  val occurredAt: Timestamp
  val applicationId: UUID?
  val assessmentId: UUID?
  val bookingId: UUID?
  val premisesId: UUID?
}
