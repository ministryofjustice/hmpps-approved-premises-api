package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import org.springframework.data.history.RevisionMetadata
import java.time.Instant
import java.util.UUID

data class RevisionSnapshot(
  val revisionNumber: Int?,
  val timestamp: Instant,
  val user: String,
  val type: EventType,
  val entityType: String,
  val entityId: UUID,
  val entity: Any?,
  val revisionType: RevisionMetadata.RevisionType,
)

enum class EventType {
  RELEASE_PLAN,
  RELEASE_ACTION,
}
