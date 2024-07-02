package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface AppealRepository : JpaRepository<AppealEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): List<AppealEntity>
  fun findAllByAssessmentId(assessmentId: UUID): List<AppealEntity>
}

@Entity
@Table(name = "appeals")
data class AppealEntity(
  @Id
  val id: UUID,
  val appealDate: LocalDate,
  val appealDetail: String,
  var decision: String,
  var decisionDetail: String,
  val createdAt: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApplicationEntity,
  @ManyToOne
  @JoinColumn(name = "assessment_id")
  var assessment: AssessmentEntity,
  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity,
  @Version
  var version: Long = 1,
)
