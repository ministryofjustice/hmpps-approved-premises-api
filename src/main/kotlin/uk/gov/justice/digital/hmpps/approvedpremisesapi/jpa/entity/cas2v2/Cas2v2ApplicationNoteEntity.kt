package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

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
interface Cas2v2ApplicationNoteRepository : JpaRepository<Cas2v2ApplicationNoteEntity, UUID> {
  @Query(
    "SELECT n FROM Cas2v2ApplicationNoteEntity n WHERE n.assessment IS NULL",
  )
  fun findAllNotesWithoutAssessment(of: PageRequest): Slice<Cas2v2ApplicationNoteEntity>
}

@Entity
@Table(name = "cas_2_v2_application_notes")
data class Cas2v2ApplicationNoteEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: Cas2v2UserEntity,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2v2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var body: String,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  var assessment: Cas2v2AssessmentEntity?,
) {

  fun getUser(): Cas2v2UserEntity = this.createdByUser

  override fun toString() = "Cas2v2ApplicationNoteEntity: $id"
}
