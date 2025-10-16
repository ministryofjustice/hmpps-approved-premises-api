package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2ApplicationNoteRepository : JpaRepository<Cas2ApplicationNoteEntity, UUID> {
  @Query(
    "SELECT n FROM Cas2ApplicationNoteEntity n WHERE n.assessment IS NULL",
  )
  fun findAllNotesWithoutAssessment(of: PageRequest): Slice<Cas2ApplicationNoteEntity>

  @Query(
    "SELECT COUNT(*) FROM Cas2ApplicationNoteEntity n WHERE n.assessment IS NULL",
  )
  fun countAllNotesWithoutAssessment(): Int

  @Query(
    "SELECT COUNT(*) FROM Cas2ApplicationNoteEntity n WHERE n.assessment IS NOT NULL",
  )
  fun countAllNotesWithAssessment(): Int
}

@Entity
@Table(name = "cas_2_application_notes")
data class Cas2ApplicationNoteEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "created_by_cas2_user_id")
  var createdByUser: Cas2UserEntity,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var body: String,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  var assessment: Cas2AssessmentEntity?,
) {
  override fun toString() = "Cas2ApplicationNoteEntity: $id"
}
