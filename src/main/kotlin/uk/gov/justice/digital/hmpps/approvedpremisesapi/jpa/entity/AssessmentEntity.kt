package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface AssessmentRepository : JpaRepository<AssessmentEntity, UUID>

@Entity
@Table(name = "assessments")
data class AssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
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
  val allocatedToUserId: UserEntity,

  val allocatedAt: OffsetDateTime,

  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,

  @Transient
  var schemaUpToDate: Boolean
)
