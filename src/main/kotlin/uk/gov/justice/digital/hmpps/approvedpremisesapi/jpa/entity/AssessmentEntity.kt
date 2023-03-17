package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AssessmentRepository : JpaRepository<AssessmentEntity, UUID> {
  fun findAllByAllocatedToUser_IdAndReallocatedAtNull(userId: UUID): List<AssessmentEntity>

  fun findAllByReallocatedAtNull(): List<AssessmentEntity>
  fun findByApplication_IdAndReallocatedAtNull(applicationId: UUID): AssessmentEntity?
}

@Entity
@Table(name = "assessments")
data class AssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
  var data: String?,

  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  val allocatedToUser: UserEntity,

  val allocatedAt: OffsetDateTime,
  var reallocatedAt: OffsetDateTime?,

  val createdAt: OffsetDateTime,

  var submittedAt: OffsetDateTime?,
  @Enumerated(value = EnumType.STRING)
  var decision: AssessmentDecision?,
  var rejectionRationale: String?,

  @OneToMany(mappedBy = "assessment")
  var clarificationNotes: MutableList<AssessmentClarificationNoteEntity>,

  @Transient
  var schemaUpToDate: Boolean
)

enum class AssessmentDecision {
  ACCEPTED,
  REJECTED
}

@Repository
interface AssessmentClarificationNoteRepository : JpaRepository<AssessmentClarificationNoteEntity, UUID> {
  fun findByAssessmentIdAndId(assessmentId: UUID, id: UUID): AssessmentClarificationNoteEntity?
}

@Entity
@Table(name = "assessment_clarification_notes")
data class AssessmentClarificationNoteEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,
  val createdAt: OffsetDateTime,

  val query: String,

  var response: String?,

  var responseReceivedOn: LocalDate?
)
