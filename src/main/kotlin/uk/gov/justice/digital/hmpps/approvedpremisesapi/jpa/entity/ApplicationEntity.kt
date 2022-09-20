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
import javax.persistence.Table

@Repository
interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  fun findAllByCreatedByProbationOfficer_Id(id: UUID): List<ApplicationEntity>
}

@Entity
@Table(name = "applications")
data class ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  @ManyToOne
  @JoinColumn(name = "created_by_probation_officer_id")
  val createdByProbationOfficer: ProbationOfficerEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  val data: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  val schemaVersion: ApplicationSchemaEntity,
  val createdAt: OffsetDateTime,
  val submittedAt: OffsetDateTime?,

  @Transient
  var schemaUpToDate: Boolean
)
