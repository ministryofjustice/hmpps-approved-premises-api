package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.UUID

interface DomainEventSummary {
  val id: String
  val type: DomainEventType
  val occurredAt: OffsetDateTime
  val applicationId: UUID?
  val assessmentId: UUID?
  val bookingId: UUID?
  val premisesId: UUID?
  val appealId: UUID?
  val triggeredByUser: UserEntity?
}
