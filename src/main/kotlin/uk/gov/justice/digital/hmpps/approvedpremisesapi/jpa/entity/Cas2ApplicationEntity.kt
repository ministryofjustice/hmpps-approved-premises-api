package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.OrderBy
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.LockModeType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface Cas2ApplicationRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    a.noms_number as nomsNumber,
    a.pnc_number as pncNumber,
    a.name,
    a.date_of_birth as dateOfBirth,
    a.nationality,
    a.sex,
    a.prison_name as prisonName,
    a.person_status as personStatus,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt
FROM cas_2_applications a
WHERE a.created_by_user_id = :userId
""",
    nativeQuery = true,
  )
  fun findAllCas2ApplicationSummariesCreatedByUser(userId: UUID):
    List<Cas2ApplicationSummary>

  @Query(
    """
SELECT
    CAST(a.id AS TEXT) as id,
    a.crn,
    a.noms_number as nomsNumber,
    a.pnc_number as pncNumber,
    a.name,
    a.date_of_birth as dateOfBirth,
    a.nationality,
    a.sex,
    a.prison_name as prisonName,
    a.person_status as personStatus,
    CAST(a.created_by_user_id AS TEXT) as createdByUserId,
    a.created_at as createdAt,
    a.submitted_at as submittedAt
FROM cas_2_applications a
WHERE a.submitted_at IS NOT NULL
""",
    nativeQuery = true,
  )
  fun findAllSubmittedCas2ApplicationSummaries(): List<Cas2ApplicationSummary>

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id AND " +
      "a.submittedAt IS NOT NULL",
  )
  fun findSubmittedApplicationById(id: UUID): Cas2ApplicationEntity?

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.createdByUser.id = :id")
  fun findAllByCreatedByUser_Id(id: UUID): List<Cas2ApplicationEntity>

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun findByIdOrNullWithWriteLock(id: UUID): Cas2ApplicationEntity?
}

@Entity
@Table(name = "cas_2_applications")
data class Cas2ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  var nomsNumber: String,

  var pncNumber: String?,

  var name: String,

  var dateOfBirth: LocalDate,

  var nationality: String?,

  var sex: String?,

  var prisonName: String?,

  var personStatus: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: NomisUserEntity,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,

  @OneToMany(mappedBy = "application")
  @OrderBy(clause = "createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,

  @Transient
  var schemaUpToDate: Boolean,

) {
  override fun toString() = "Cas2ApplicationEntity: $id"
}

@Suppress("TooManyFunctions")
interface AppSummary {
  fun getId(): UUID
  fun getCrn(): String
  fun getCreatedByUserId(): UUID
  fun getCreatedAt(): Timestamp
  fun getSubmittedAt(): Timestamp?
  fun getNomsNumber(): String
  fun getPncNumber(): String?
  fun getName(): String
  fun getDateOfBirth(): LocalDate
  fun getNationality(): String?
  fun getSex(): String
  fun getPrisonName(): String?
  fun getPersonStatus(): String
  fun getPerson(): FullPerson
}

interface Cas2ApplicationSummary : AppSummary {
  override fun getPerson(): FullPerson {
    return FullPerson(
      type = PersonType.fullPerson,
      crn = this.getCrn(),
      nomsNumber = this.getNomsNumber(),
      pncNumber = this.getPncNumber(),
      name = this.getName(),
      dateOfBirth = this.getDateOfBirth(),
      nationality = this.getNationality(),
      sex = this.getSex(),
      prisonName = this.getPrisonName(),
      status = FullPerson.Status.valueOf(this.getPersonStatus()),
    )
  }
}
