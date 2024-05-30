package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.OrderBy
import org.hibernate.annotations.Type
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.LockModeType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Suppress("TooManyFunctions")
@Repository
interface Cas2ApplicationRepository : JpaRepository<Cas2ApplicationEntity, UUID> {
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

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.submittedAt IS NOT NULL " +
      "AND a.id NOT IN (SELECT application FROM Cas2AssessmentEntity)",
  )
  fun findAllSubmittedApplicationsWithoutAssessments(): Slice<Cas2ApplicationEntity>
}

@Entity
@Table(name = "cas_2_applications")
data class Cas2ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

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
  var abandonedAt: OffsetDateTime? = null,

  @OneToMany(mappedBy = "application")
  @OrderBy(clause = "createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,

  @OneToMany(mappedBy = "application")
  @OrderBy(clause = "createdAt DESC")
  var notes: MutableList<Cas2ApplicationNoteEntity>? = null,

  @OneToOne(mappedBy = "application")
  var assessment: Cas2AssessmentEntity? = null,

  @Transient
  var schemaUpToDate: Boolean,

  var nomsNumber: String?,

  var referringPrisonCode: String? = null,
  var preferredAreas: String? = null,
  var hdcEligibilityDate: LocalDate? = null,
  var conditionalReleaseDate: LocalDate? = null,
  var telephoneNumber: String? = null,
) {
  override fun toString() = "Cas2ApplicationEntity: $id"
}
