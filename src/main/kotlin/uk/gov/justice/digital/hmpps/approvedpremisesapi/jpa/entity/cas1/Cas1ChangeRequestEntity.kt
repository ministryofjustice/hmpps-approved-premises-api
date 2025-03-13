package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1ChangeRequestRepository : JpaRepository<Cas1ChangeRequestEntity, UUID>

@Entity
@Table(name = "cas1_change_requests")
data class Cas1ChangeRequestEntity(
  @Id
  val id: UUID,
  /**
   * A change request is logically owned by the
   * placement request (_not_ the linked space booking)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_space_booking_id")
  val spaceBooking: Cas1SpaceBookingEntity,
  @Enumerated(EnumType.STRING)
  val type: ChangeRequestType,
  val requestJson: String?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_change_request_reason_id")
  val requestReason: Cas1ChangeRequestReasonEntity,
  var decisionJson: String?,
  @Enumerated(EnumType.STRING)
  var decision: ChangeRequestDecision,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_change_request_rejection_reason_id")
  val rejectionReason: Cas1ChangeRequestRejectionReasonEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decision_made_by_user_id")
  val decisionMadeByUser: UserEntity,
  val createdAt: OffsetDateTime,
  var updatedAt: OffsetDateTime,
  var resolvedAt: OffsetDateTime?,
  @Version
  var version: Long = 1,
) {

  @PreUpdate
  fun preUpdate() {
    updatedAt = OffsetDateTime.now()
  }
}

enum class ChangeRequestDecision {
  APPROVED,
  REJECTED,
}

enum class ChangeRequestType {
  APPEAL,
  EXTENSION,
  PLANNED_TRANSFER,
}
