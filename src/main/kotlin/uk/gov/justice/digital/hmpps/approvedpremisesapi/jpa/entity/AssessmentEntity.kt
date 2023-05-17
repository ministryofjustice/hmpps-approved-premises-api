package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.ColumnResult
import javax.persistence.ConstructorResult
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedNativeQuery
import javax.persistence.OneToMany
import javax.persistence.SqlResultSetMapping
import javax.persistence.Table

@Repository
interface AssessmentRepository : JpaRepository<AssessmentEntity, UUID> {
  @Query(nativeQuery = true)
  fun findAllAssessmentSummariesNotReallocated(userIdString: String? = null): List<DomainAssessmentSummary>

  fun findAllByReallocatedAtNullAndSubmittedAtNull(): List<AssessmentEntity>
  fun findByApplication_IdAndReallocatedAtNull(applicationId: UUID): AssessmentEntity?
}

@NamedNativeQuery(
  name = "AssessmentEntity.findAllAssessmentSummariesNotReallocated",
  query =
  """
    select service as type,
           cast(a.id as text) as id,
           cast(a.application_id as text) as applicationId,
           a.created_at as createdAt,
           CAST(apa.risk_ratings AS TEXT) as riskRatings,
           apa.arrival_date as arrivalDate,
           (
            select min(acn.created_at)
             from  assessment_clarification_notes acn
             where acn.response is null
                   and acn.assessment_id = a.id
           ) as dateOfInfoRequest,
           a.decision is not null as completed,
           a.decision as decision,
           ap.crn as crn
      from assessments a
           join applications ap on a.application_id = ap.id
           left outer join approved_premises_applications apa on ap.id = apa.id
     where a.reallocated_at is null
           and (?1 is null or a.allocated_to_user_id = cast(?1 as UUID))
    """,
  resultSetMapping = "DomainAssessmentSummaryMapping"
)

@SqlResultSetMapping(
  name = "DomainAssessmentSummaryMapping",
  classes = [
    ConstructorResult(
      targetClass = DomainAssessmentSummary::class,
      columns = [
        ColumnResult(name = "type"),
        ColumnResult(name = "id", type = UUID::class),
        ColumnResult(name = "applicationId", type = UUID::class),
        ColumnResult(name = "createdAt", type = OffsetDateTime::class),
        ColumnResult(name = "riskRatings"),
        ColumnResult(name = "arrivalDate", type = OffsetDateTime::class),
        ColumnResult(name = "dateOfInfoRequest", type = OffsetDateTime::class),
        ColumnResult(name = "completed"),
        ColumnResult(name = "decision"),
        ColumnResult(name = "crn"),
      ]
    )
  ]
)

@Entity
@Table(name = "assessments")
data class AssessmentEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
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

/**
 * Summary data for an assessment - read-only, to be used when retrieving large numbers of Assessments.
 * Hibernate compatible equals, hash code and toString aren't needed.
 */
open class DomainAssessmentSummary(
  val type: String,
  val id: UUID,
  val applicationId: UUID,
  val createdAt: OffsetDateTime,
  val riskRatings: String?,
  val arrivalDate: OffsetDateTime?,
  val dateOfInfoRequest: OffsetDateTime?,
  val completed: Boolean,
  val decision: String?,
  val crn: String
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
