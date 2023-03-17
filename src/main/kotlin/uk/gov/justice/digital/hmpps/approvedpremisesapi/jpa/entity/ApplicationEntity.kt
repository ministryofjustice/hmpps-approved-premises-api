package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {
  @Query("SELECT a FROM ApplicationEntity a WHERE TYPE(a) = :type AND a.createdByUser.id = :id")
  fun <T : ApplicationEntity> findAllByCreatedByUser_Id(id: UUID, type: Class<T>): List<ApplicationEntity>

  @Query(
    "SELECT a FROM ApplicationEntity a " +
      "LEFT JOIN ApplicationTeamCodeEntity atc ON a = atc.application " +
      "WHERE TYPE(a) = :type AND atc.teamCode IN (:managingTeamCodes)"
  )
  fun <T : ApplicationEntity> findAllByManagingTeam(managingTeamCodes: List<String>, type: Class<T>): List<ApplicationEntity>

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

  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
  var data: String?,

  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
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
  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
  @Convert(disableConversion = true)
  val riskRatings: PersonRisks?,
  @OneToMany(mappedBy = "application")
  val teamCodes: MutableList<ApplicationTeamCodeEntity>
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
) {
  fun hasTeamCode(code: String) = teamCodes.any { it.teamCode == code }
  fun hasAnyTeamCode(codes: List<String>) = codes.any(::hasTeamCode)
}

@Repository
interface ApplicationTeamCodeRepository : JpaRepository<ApplicationTeamCodeEntity, UUID>

@Entity
@Table(name = "approved_premises_application_team_codes")
data class ApplicationTeamCodeEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,
  val teamCode: String
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
