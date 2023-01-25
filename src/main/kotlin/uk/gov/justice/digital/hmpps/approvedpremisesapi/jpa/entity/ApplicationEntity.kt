package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Convert
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.Table

@Repository
interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query("SELECT a FROM ApplicationEntity a WHERE TYPE(a) = :type AND a.createdByUser.id = :id")
  fun <T : ApplicationEntity> findAllByCreatedByUser_Id(id: UUID, type: Class<T>): List<ApplicationEntity>

  @Query("SELECT a FROM ApplicationEntity a WHERE TYPE(a) = :type AND a.crn = :crn")
  fun <T : ApplicationEntity> findByCrn(crn: String, type: Class<T>): List<ApplicationEntity>
}

@Entity
@Table(name = "applications")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,

  @Transient
  var schemaUpToDate: Boolean,

  @OneToMany(mappedBy = "application")
  var assessments: MutableList<AssessmentEntity>,
) {
  fun getLatestAssessment(): AssessmentEntity? = this.assessments.maxByOrNull { it.createdAt }
}

@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises_applications")
@PrimaryKeyJoinColumn(name = "id")
class ApprovedPremisesApplicationEntity(
  id: UUID,
  crn: String,
  createdByUser: UserEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  schemaUpToDate: Boolean,
  assessments: MutableList<AssessmentEntity>,
  var isWomensApplication: Boolean?,
  var isPipeApplication: Boolean?,
  val convictionId: Long,
  val eventNumber: String,
  val offenceId: String,
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  @Convert(disableConversion = true)
  val riskRatings: PersonRisks?
) : ApplicationEntity(
  id,
  crn,
  createdByUser,
  data,
  document,
  schemaVersion,
  createdAt,
  submittedAt,
  schemaUpToDate,
  assessments,
)

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_applications")
@PrimaryKeyJoinColumn(name = "id")
class TemporaryAccommodationApplicationEntity(
  id: UUID,
  crn: String,
  createdByUser: UserEntity,
  data: String?,
  document: String?,
  schemaVersion: JsonSchemaEntity,
  createdAt: OffsetDateTime,
  submittedAt: OffsetDateTime?,
  schemaUpToDate: Boolean,
  assessments: MutableList<AssessmentEntity>,
) : ApplicationEntity(
  id,
  crn,
  createdByUser,
  data,
  document,
  schemaVersion,
  createdAt,
  submittedAt,
  schemaUpToDate,
  assessments
)
