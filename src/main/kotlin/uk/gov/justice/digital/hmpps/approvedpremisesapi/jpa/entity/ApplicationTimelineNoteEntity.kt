package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

interface ApplicationTimelineNoteRepository : JpaRepository<ApplicationTimelineNoteEntity, UUID> {

  fun findApplicationTimelineNoteEntitiesByApplicationId(applicationId: UUID): List<ApplicationTimelineNoteEntity>
}

@Entity
@Table(name = "application_timeline_notes")
data class ApplicationTimelineNoteEntity(
  @Id
  val id: UUID,
  val applicationId: UUID,
  val createdBy: UUID,
  val createdAt: OffsetDateTime,
  val body: String,
)
