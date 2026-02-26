package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Type
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacementStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1PlacementDates
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision as ApiPlacementApplicationDecision

@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementApplicationRepository : JpaRepository<PlacementApplicationEntity, UUID> {

  @Query(
    """
      SELECT a from PlacementApplicationEntity a where a.application.id = :applicationId
      AND a.submittedAt is not null
      AND a.reallocatedAt is null
    """,
  )
  fun findAllSubmittedNonReallocatedApplicationsForApplicationId(applicationId: UUID): List<PlacementApplicationEntity>

  fun findByApplication(application: ApplicationEntity): List<PlacementApplicationEntity>

  @Query("SELECT p from PlacementApplicationEntity p WHERE p.dueAt IS NULL AND p.submittedAt IS NOT NULL")
  fun findAllWithNullDueAt(pageable: Pageable?): Slice<PlacementApplicationEntity>

  @Modifying
  @Query("UPDATE PlacementApplicationEntity p SET p.dueAt = :dueAt WHERE p.id = :id")
  fun updateDueAt(id: UUID, dueAt: OffsetDateTime?)
}

@Repository
interface LockablePlacementApplicationRepository : JpaRepository<LockablePlacementApplicationEntity, UUID> {
  @Query("SELECT a FROM LockablePlacementApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): LockablePlacementApplicationEntity?
}

/**
 * Represents a Request for Placement, either included in the original application
 * (with some caveats, see below), or made after the application has been assessed
 *
 * 'automatic' request for placements are those included in the original application.
 * These [PlacementApplicationEntity]s are created when that application is assessed
 * and accepted. Note - these were only created as of 26/8/25.
 * See [PlacementRequestEntity.isForLegacyInitialRequestForPlacement] for more information.
 *
 */
@Entity
@Table(name = "placement_applications")
data class PlacementApplicationEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: UserEntity,

  @Type(JsonType::class)
  var data: String?,

  @Type(JsonType::class)
  var document: String?,

  // Note that this is mapped to timestamp. It should be timestamptz
  val createdAt: OffsetDateTime,

  // Note that this is mapped to timestamp. It should be timestamptz
  var submittedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  // Note that this is mapped to timestamp. It should be timestamptz
  var allocatedAt: OffsetDateTime?,
  // Note that this is mapped to timestamp. It should be timestamptz
  var reallocatedAt: OffsetDateTime?,

  @Enumerated(value = EnumType.STRING)
  var decision: PlacementApplicationDecision?,

  var decisionMadeAt: OffsetDateTime?,

  var placementType: PlacementType?,

  /**
   * If true, this Placement Application was created on assessment of
   * an application that included a placement date, and represents the
   * request for placement implicit in that original application
   *
   * Automatic applications do not go through the regular placement
   * applications review process (they're approved on creation),
   * or appear as completed tasks
   *
   * These type of requests for placemnets were only created as of 26/8/25.
   * See [PlacementRequestEntity.isForLegacyInitialRequestForPlacement] for
   * more information
   */
  val automatic: Boolean,

  @OneToOne(mappedBy = "placementApplication", fetch = FetchType.LAZY)
  var placementRequest: PlacementRequestEntity?,

  @Enumerated(value = EnumType.STRING)
  var withdrawalReason: PlacementApplicationWithdrawalReason?,

  var dueAt: OffsetDateTime?,

  @Column(name = "placement_application_submission_group_id")
  var submissionGroupId: UUID?,

  var isWithdrawn: Boolean = false,

  /**
   * If [submittedAt] is not null, this value will be set. Use [placementDates()] to access.
   */
  var expectedArrival: LocalDate? = null,

  var requestedDuration: Int? = null,

  var authorisedDuration: Int? = null,

  var expectedArrivalFlexible: Boolean? = null,

  var releaseType: String? = null,
  var sentenceType: String? = null,
  var situation: String? = null,

  val backfilledAutomatic: Boolean = false,

  @Version
  var version: Long = 1,
) {
  fun isReallocated() = reallocatedAt != null

  fun isActive() = !isReallocated() && !isWithdrawn

  fun isInWithdrawableState() = isSubmitted() && isActive()

  fun isSubmitted() = submittedAt != null

  fun isBeingAssessed() = isActive() && decision == null

  fun placementDates() = if (isSubmitted()) {
    Cas1PlacementDates(
      expectedArrival = expectedArrival!!,
      duration = authorisedDuration ?: requestedDuration!!,
    )
  } else {
    null
  }

  fun deriveStatus(): RequestForPlacementStatus = when {
    this.isWithdrawn -> RequestForPlacementStatus.requestWithdrawn
    this.placementRequest?.hasActiveBooking() == true -> RequestForPlacementStatus.placementBooked
    this.decision == PlacementApplicationDecision.REJECTED -> RequestForPlacementStatus.requestRejected
    this.decision == PlacementApplicationDecision.ACCEPTED -> RequestForPlacementStatus.awaitingMatch
    this.isSubmitted() -> RequestForPlacementStatus.requestSubmitted
    else -> RequestForPlacementStatus.requestUnsubmitted
  }

  override fun toString() = "PlacementApplicationEntity: $id"
}

/**
 * Provides a version of the AssessmentEntity with no relationships, allowing
 * us to lock the applications table only without JPA/Hibernate attempting to
 * lock all eagerly loaded relationships
 */
@Entity
@Table(name = "assessments")
@Immutable
class LockablePlacementApplicationEntity(
  @Id
  val id: UUID,
)

// Do not re-order these elements as we currently use ordinal enum mapping in hibernate
// (i.e. they're persisted as index numbers, not enum name strings)
enum class PlacementType {
  ROTL,
  RELEASE_FOLLOWING_DECISION,
  ADDITIONAL_PLACEMENT,
  AUTOMATIC,
}

enum class PlacementApplicationDecision(val apiValue: ApiPlacementApplicationDecision) {
  ACCEPTED(ApiPlacementApplicationDecision.accepted),
  REJECTED(ApiPlacementApplicationDecision.rejected),

  /**
   * @deprecated isWithdrawn property supersedes the use of these two values.
   * Previous versions of the code would overwrite the decision to WITHDRAW when the PP decided to
   * withdraw a placement_application. Furthermore, an assessor could mark an assessment as either
   * WITHDRAW/WITHDRAWN_BY_PP when assessing a placement_application.
   *
   * We now maintain separate fields to capture withdrawal state, as to not override the decision made by the assessor,
   * and assessors can no longer mark a placement_application as WITHDRAW/WITHDRAWN_BY_PP
   *
   * Because we don’t have sufficient information to determine what an assessors decision was once a
   * placement_application has been withdrawn, we couldn’t backfill the ‘decision’ column to replace
   * WITHDRAW/WITHDRAWN_BY_PP to be either ACCEPTED or REJECTED with the correct decision_made_at value.
   * For this reason, legacy placement_applications may still have a decision value of WITHDRAW/WITHDRAWN_BY_PP which
   * is a helpful indicator that the decision_made_at date should not be trusted
   */
  @Deprecated("Explicit isWithdrawn property supersedes this value")
  WITHDRAW(ApiPlacementApplicationDecision.withdraw),

  @Deprecated("Explicit isWithdrawn property supersedes this value")
  WITHDRAWN_BY_PP(ApiPlacementApplicationDecision.withdrawnByPp),
  ;

  companion object {
    fun valueOf(apiValue: ApiPlacementApplicationDecision): PlacementApplicationDecision? {
      for (e in entries) {
        if (e.apiValue == apiValue) {
          return e
        }
      }
      return null
    }
  }
}

enum class PlacementApplicationWithdrawalReason(val apiValue: WithdrawPlacementRequestReason) {
  DUPLICATE_PLACEMENT_REQUEST(WithdrawPlacementRequestReason.duplicatePlacementRequest),
  ALTERNATIVE_PROVISION_IDENTIFIED(WithdrawPlacementRequestReason.alternativeProvisionIdentified),
  WITHDRAWN_BY_PP(WithdrawPlacementRequestReason.withdrawnByPP),
  CHANGE_IN_CIRCUMSTANCES(WithdrawPlacementRequestReason.changeInCircumstances),
  CHANGE_IN_RELEASE_DECISION(WithdrawPlacementRequestReason.changeInReleaseDecision),
  NO_CAPACITY_DUE_TO_LOST_BED(WithdrawPlacementRequestReason.noCapacityDueToLostBed),
  NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION(WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation),
  NO_CAPACITY(WithdrawPlacementRequestReason.noCapacity),
  ERROR_IN_PLACEMENT_REQUEST(WithdrawPlacementRequestReason.errorInPlacementRequest),
  RELATED_APPLICATION_WITHDRAWN(WithdrawPlacementRequestReason.relatedApplicationWithdrawn),
  ;

  companion object {
    fun valueOf(apiValue: WithdrawPlacementRequestReason): PlacementApplicationWithdrawalReason? = entries.firstOrNull { it.apiValue == apiValue }
  }
}
