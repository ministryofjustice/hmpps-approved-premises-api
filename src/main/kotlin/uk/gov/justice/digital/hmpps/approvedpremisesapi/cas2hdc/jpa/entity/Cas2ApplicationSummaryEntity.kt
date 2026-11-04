package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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

@Repository
interface Cas2ApplicationSummaryRepository :
  JpaRepository<Cas2ApplicationSummaryEntity, String>,
  JpaSpecificationExecutor<Cas2ApplicationSummaryEntity> {
  @Query("select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is null and ase.userId = :userId and ase.serviceOrigin = :serviceOrigin")
  fun findInProgressApplications(userId: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.allocatedPomUserId = :userId and ase.serviceOrigin = :serviceOrigin",
  )
  fun findApplicationsAssignedToUser(userId: UUID, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.currentPrisonCode = :prisonCode and ase.allocatedPomUserId is not null and ase.serviceOrigin = :serviceOrigin",
  )
  fun findAllocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummaryEntity>

  @Query(
    "select ase from Cas2ApplicationSummaryEntity ase where ase.submittedAt is not null " +
      "and ase.currentPrisonCode = :prisonCode and ase.allocatedPomUserId is null and ase.serviceOrigin = :serviceOrigin",
  )
  fun findUnallocatedApplicationsInSamePrisonAsUser(prisonCode: String, pageable: Pageable, serviceOrigin: String): Page<Cas2ApplicationSummaryEntity>

  fun findAllByServiceOriginAndIdIn(serviceOrigin: String, id: List<UUID>, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>

  fun findByServiceOriginAndSubmittedAtIsNotNull(serviceOrigin: String, pageable: Pageable): Page<Cas2ApplicationSummaryEntity>
}

/**
 * Backed by a View
 *
 * The WHERE clause in the query includes an application if:
 *
 * 1. it is not abandoned, it has no status and (conditional release date is null or >= today)
 * 2. it has a status that isn't 'withdrawn', 'awaiting arrival' or 'referral cancelled'
 * 3. it has a status that is 'withdrawn', 'awaiting arrival' or 'referral cancelled', but this status was set no more than 32 days ago
 *
 * Noting that an application can only be abandoned _before_ submission, the logic can be simplified as _not_ showing applications when:
 *
 * 1. The application is abandoned before submission
 * 2. The application has no status and a conditional release date >= today
 * 3. The application has a status that is 'withdrawn', 'awaiting arrival' or 'referral cancelled' set over 32 days ago
 *
 * Note that point 2 is mostly likely a bug, because as soon as a status is set it'll become visible again
 */
@Entity
@Table(name = "cas_2_application_live_summary")
data class Cas2ApplicationSummaryEntity(
  @Id
  val id: UUID,
  val crn: String,
  @Column(name = "noms_number")
  var nomsNumber: String,
  @Column(name = "created_by_cas2_user_id")
  val userId: String,
  @Column(name = "created_by_cas2_user_name")
  val userName: String,
  @Column(name = "allocated_pom_cas_2_user_id")
  val allocatedPomUserId: UUID?,
  @Column(name = "allocated_pom_cas_2_name")
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
  @Column(name = "service_origin")
  var serviceOrigin: String? = null,
  @Column(name = "cohort")
  @Enumerated(EnumType.STRING)
  var cohort: Cas2Cohort? = null,
)
