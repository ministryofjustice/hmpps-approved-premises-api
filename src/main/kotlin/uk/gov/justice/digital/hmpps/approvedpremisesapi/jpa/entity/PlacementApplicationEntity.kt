package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.Type
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementApplicationRepository : JpaRepository<PlacementApplicationEntity, UUID> {

  @Query(
    """
    SELECT
      pa
    FROM
      PlacementApplicationEntity pa
    where
      pa.submittedAt IS NOT NULL AND
      pa.reallocatedAt IS NULL AND
      pa.decision IS NULL AND 
      (
        (:#{#allocationStatus?.toString()} = 'ALLOCATED' AND pa.allocatedToUser IS NOT NULL) OR
        (:#{#allocationStatus?.toString()} = 'UNALLOCATED' AND pa.allocatedToUser IS NULL) OR
        (:#{#allocationStatus?.toString()} = 'EITHER')
      )
    """,
  )
  fun findByAllocationStatus(allocationStatus: AllocationStatus, pageable: Pageable?): Page<PlacementApplicationEntity>

  enum class AllocationStatus {
    ALLOCATED,
    UNALLOCATED,
    EITHER,
  }

  @Query(
    """
      SELECT pa FROM PlacementApplicationEntity pa
      JOIN pa.application a
      LEFT OUTER JOIN a.apArea apArea
      WHERE 
        pa.allocatedToUser.id = :userId AND 
        ((cast(:apAreaId as org.hibernate.type.UUIDCharType) IS NULL) OR apArea.id = :apAreaId) AND
        pa.reallocatedAt IS NULL AND 
        pa.decision IS NULL
    """,
  )
  fun findOpenRequestsAssignedToUser(
    userId: UUID,
    apAreaId: UUID?,
    pageable: Pageable?,
  ): Page<PlacementApplicationEntity>

  @Query(
    """
      SELECT a from PlacementApplicationEntity a where a.application.id = :applicationId
      AND a.submittedAt is not null
      AND a.reallocatedAt is null
      AND 
        (
            a.decision != uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.WITHDRAWN_BY_PP
            OR
            a.decision is null
        )
    """,
  )
  fun findAllSubmittedNonReallocatedAndNonWithdrawnApplicationsForApplicationId(applicationId: UUID): List<PlacementApplicationEntity>
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

  @OneToMany(mappedBy = "placementApplication")
  var placementDates: MutableList<PlacementDateEntity>,

  @OneToMany(mappedBy = "placementApplication")
  var placementRequests: MutableList<PlacementRequestEntity>,
) {
  fun canBeWithdrawn() = placementRequests.all { it.booking == null }

  override fun toString() = "PlacementApplicationEntity: $id"
}

enum class PlacementType {
  ROTL,
  RELEASE_FOLLOWING_DECISION,
  ADDITIONAL_PLACEMENT,
}

enum class PlacementApplicationDecision {
  ACCEPTED,
  REJECTED,
  WITHDRAW,
  WITHDRAWN_BY_PP,
}
