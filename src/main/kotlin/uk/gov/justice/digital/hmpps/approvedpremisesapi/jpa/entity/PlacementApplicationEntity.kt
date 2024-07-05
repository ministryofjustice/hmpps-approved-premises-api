package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Version
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

  @Query(
    """
    SELECT cast(pa.application_id as text) 
    FROM placement_applications pa 
    LEFT OUTER JOIN placement_requests pr ON pr.placement_application_id = pa.id
    WHERE pa.decision = 'ACCEPTED' AND pr.id IS NULL
    GROUP BY pa.application_id
    """,
    nativeQuery = true,
  )
  fun findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest(): List<String>
}

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

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,

  @Transient
  var schemaUpToDate: Boolean,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var data: String?,

  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  var document: String?,

  val createdAt: OffsetDateTime,

  var submittedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  var allocatedAt: OffsetDateTime?,
  var reallocatedAt: OffsetDateTime?,

  @Enumerated(value = EnumType.STRING)
  var decision: PlacementApplicationDecision?,

  var decisionMadeAt: OffsetDateTime?,

  var placementType: PlacementType?,

  /**
   * Supporting multiple dates in a single Placement Application is legacy behaviour. Any new
   * placement application will have one and only one date in this collection
   */
  @OneToMany(mappedBy = "placementApplication")
  var placementDates: MutableList<PlacementDateEntity>,

  /**
   * Supporting multiple placements requests in a single Placement Application is legacy behaviour. Any new
   * placement application will have one and only one Placement Request in this collection
   */
  @OneToMany(mappedBy = "placementApplication")
  var placementRequests: MutableList<PlacementRequestEntity>,

  @Enumerated(value = EnumType.STRING)
  var withdrawalReason: PlacementApplicationWithdrawalReason?,

  var dueAt: OffsetDateTime?,

  @Column(name = "placement_application_submission_group_id")
  var submissionGroupId: UUID?,

  var isWithdrawn: Boolean = false,

  @Version
  var version: Long = 1,
) {
  fun isReallocated() = reallocatedAt != null

  fun isActive() = !isReallocated() && !isWithdrawn

  fun isAccepted() = decision == PlacementApplicationDecision.ACCEPTED

  fun isInWithdrawableState() = isSubmitted() && isActive()

  fun isSubmitted() = submittedAt != null

  fun isBeingAssessed() = isActive() && decision == null

  override fun toString() = "PlacementApplicationEntity: $id"
}

// Do not re-order these elements as we currently use ordinal enum mapping in hibernate
// (i.e. they're persisted as index numbers, not enum name strings)
enum class PlacementType {
  ROTL,
  RELEASE_FOLLOWING_DECISION,
  ADDITIONAL_PLACEMENT,
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
    fun valueOf(apiValue: WithdrawPlacementRequestReason): PlacementApplicationWithdrawalReason? =
      PlacementApplicationWithdrawalReason.entries.firstOrNull { it.apiValue == apiValue }
  }
}
