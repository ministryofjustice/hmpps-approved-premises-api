package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// TODO besscerule change name of interface with CAS2
@Repository
interface ApplicationSummaryRepository :
  JpaRepository<Cas2ApplicationSummaryEntity, String>,
  JpaSpecificationExecutor<Cas2ApplicationSummaryEntity> {
  @Query("select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is null and ase.userId = :userId")
  fun findInProgressApplications(userId: UUID, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.allocatedPomUserId = :userId",
  )
  fun findApplicationsAssignedToUser(userId: UUID, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.currentPrisonCode = :prisonCode and ase.allocatedPomUserId is not null",
  )
  fun findAllocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.currentPrisonCode = :prisonCode and ase.allocatedPomUserId is null",
  )
  fun findUnallocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  fun findAllByIdIn(ids: List<UUID>, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  fun findBySubmittedAtIsNotNull(pageable: Pageable?): Page<Cas2ApplicationSummaryEntity>
}

@Entity
@Table(name = "cas_2_application_live_summary")
data class Cas2ApplicationSummaryEntity(
  @Id
  val id: UUID,
  val crn: String,
  @Column(name = "noms_number")
  var nomsNumber: String,
  // TODO besscerule changing type seems like a good idea, unless it is not actually the UUID and is a different kind of ID
  @Column(name = "created_by_user_id")
  val userId: UUID,
  @Column(name = "name")
  val userName: String,
  @Column(name = "allocated_pom_user_id")
  val allocatedPomUserId: UUID?,
  @Column(name = "allocated_pom_name")
  val allocatedPomName: String?,
  @Column(name = "created_at")
  val createdAt: OffsetDateTime,
  @Column(name = "submitted_at")
  var submittedAt: OffsetDateTime?,
  @Column(name = "abandoned_at")
  var abandonedAt: OffsetDateTime? = null,
  @Column(name = "hdc_eligibility_date")
  var hdcEligibilityDate: LocalDate? = null,
  @Column(name = "label")
  var latestStatusUpdateLabel: String? = null,
  @Column(name = "status_id")
  var latestStatusUpdateStatusId: String? = null,
  @Column(name = "referring_prison_code")
  val prisonCode: String,
  @Column(name = "current_prison_code")
  val currentPrisonCode: String?,
  @Column(name = "assignment_date")
  val assignmentDate: OffsetDateTime?,
  @Column(name = "bail_hearing_date")
  var bailHearingDate: LocalDate? = null,
  @Column(name = "application_origin")
  var applicationOrigin: String? = null,
)
