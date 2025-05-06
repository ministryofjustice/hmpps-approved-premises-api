package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.SQLOrder
import org.hibernate.annotations.Type
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("TooManyFunctions")
@Repository
interface Cas2ApplicationRepository : JpaRepository<Cas2ApplicationEntity, UUID> {

  fun findFirstByNomsNumberAndSubmittedAtIsNotNullOrderBySubmittedAtDesc(nomsNumber: String): Cas2ApplicationEntity?

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.id = :id AND " +
      "a.submittedAt IS NOT NULL",
  )
  fun findSubmittedApplicationById(id: UUID): Cas2ApplicationEntity?

  @Query("SELECT a FROM Cas2ApplicationEntity a WHERE a.createdByUser.id = :id")
  fun findAllByCreatedByUserId(id: UUID): List<Cas2ApplicationEntity>

  @Query(
    "SELECT a FROM Cas2ApplicationEntity a WHERE a.submittedAt IS NOT NULL " +
      "AND a NOT IN (SELECT application FROM Cas2AssessmentEntity)",
  )
  fun findAllSubmittedApplicationsWithoutAssessments(): Slice<Cas2ApplicationEntity>

  @Query(
    """
        with 
        assignments_ordered as (
                                select aa.*,
                                row_number() over (partition by application_id order by created_at desc) as row_number
                                from cas_2_application_assignments aa),
        current_prisons as (
                                select application_id, 
                                prison_code as code
                                from assignments_ordered  where row_number = 1)
                                
        select application.id
        from cas_2_applications application
                 join assignments_ordered assignment on application.id = assignment.application_id
                 join current_prisons current_prison on assignment.application_id = current_prison.application_id
        where assignment.allocated_pom_user_id = :userId
        --find rows where the POM has been assigned previously, but is not the current POM
        and assignment.row_number > 1
        --and where the new POM is NOT in the same prison
        and current_prison.code <> :userPrisonCode;""",
    nativeQuery = true,
  )
  fun findPreviouslyAssignedApplicationsInDifferentPrisonToUser(userId: UUID, userPrisonCode: String): List<UUID>
}

@Repository
interface Cas2LockableApplicationRepository : JpaRepository<Cas2LockableApplicationEntity, UUID> {
  @Query("SELECT a FROM Cas2LockableApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): Cas2LockableApplicationEntity?
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

  @Type(JsonType::class)
  var data: String?,

  @Type(JsonType::class)
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,
  var abandonedAt: OffsetDateTime? = null,

  @OneToMany(mappedBy = "application")
  @SQLOrder("createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,

  @OneToMany(mappedBy = "application")
  @SQLOrder("createdAt DESC")
  var notes: MutableList<Cas2ApplicationNoteEntity>? = null,

  @OneToOne(mappedBy = "application")
  var assessment: Cas2AssessmentEntity? = null,

  @OneToMany(mappedBy = "application", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @SQLOrder("createdAt DESC")
  val applicationAssignments: MutableList<Cas2ApplicationAssignmentEntity> = mutableListOf(),

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
  val currentPrisonCode: String?
    get() = applicationAssignments.maxByOrNull { it.createdAt }?.prisonCode
  val currentPomUserId: UUID?
    get() = applicationAssignments.maxByOrNull { it.createdAt }?.allocatedPomUser?.id
  val currentAssignmentDate: LocalDate?
    get() = applicationAssignments.maxByOrNull { it.createdAt }?.createdAt?.toLocalDate()
  val mostRecentLocationAssignment: Cas2ApplicationAssignmentEntity?
    get() = applicationAssignments.firstOrNull { it.allocatedPomUser == null }
  val hasLocationChangedAssignment: Boolean
    get() = mostRecentLocationAssignment != null

  fun isTransferredApplication() = applicationAssignments.map { it.prisonCode }.distinct().size > 1
  fun createApplicationAssignment(prisonCode: String, allocatedPomUser: NomisUserEntity?) {
    this.applicationAssignments.add(
      Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = this,
        prisonCode = prisonCode,
        allocatedPomUser = allocatedPomUser,
        createdAt = OffsetDateTime.now(),
      ),
    )
  }

  fun isMostRecentStatusUpdateANonAssignableStatus() = statusUpdates?.firstOrNull()
    ?.let { mostRecent -> mostRecent.label in Cas2StatusUpdateNonAssignable.entries.map { it.label } }
    ?: false
}

/**
 * Provides a version of the Cas2ApplicationEntity with no relationships, allowing
 * us to lock the applications table only without JPA/Hibernate attempting to
 * lock all eagerly loaded relationships
 */
@Entity
@Table(name = "cas_2_applications")
@Immutable
class Cas2LockableApplicationEntity(
  @Id
  val id: UUID,
)
