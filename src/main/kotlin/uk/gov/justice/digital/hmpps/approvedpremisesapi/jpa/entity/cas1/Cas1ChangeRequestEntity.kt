package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import io.hypersistence.utils.hibernate.type.json.JsonType
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
import org.hibernate.annotations.Type
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1ChangeRequestRepository : JpaRepository<Cas1ChangeRequestEntity, UUID> {

  @Query(
    value = """
      WITH results AS (
        SELECT 
          cr.id as id,
          apa.name as name,
          booking.crn as crn,
          cr.type as type,
          cr.created_at as createdAt,
          (booking.canonical_departure_date - booking.canonical_arrival_date) as lengthOfStayDays,
          apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
          booking.canonical_arrival_date as canonicalArrivalDate,
          booking.expected_arrival_date as expectedArrivalDate,
          booking.actual_arrival_date as actualArrivalDate
        FROM cas1_change_requests cr
        INNER JOIN cas1_space_bookings booking on cr.cas1_space_booking_id = booking.id
        INNER JOIN placement_requests pr on cr.placement_request_id = pr.id
        INNER JOIN approved_premises_applications apa on apa.id = pr.application_id
        WHERE
          cr.decision IS NULL AND
          ((CAST(:cruManagementAreaId AS pg_catalog.uuid) IS NULL) OR apa.cas1_cru_management_area_id = :cruManagementAreaId)
      )
      select * from results  
    """,
    nativeQuery = true,
  )
  fun findOpen(
    cruManagementAreaId: UUID?,
    pageable: Pageable,
  ): List<FindOpenChangeRequestResult>

  interface FindOpenChangeRequestResult {
    val id: UUID
    val crn: String
    val type: String
    val createdAt: java.time.Instant
    val lengthOfStayDays: Int
    val tier: String
    val expectedArrivalDate: LocalDate?
    val actualArrivalDate: LocalDate?
  }
}

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
  @Type(JsonType::class)
  val requestJson: String,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_change_request_reason_id")
  val requestReason: Cas1ChangeRequestReasonEntity,
  @Type(JsonType::class)
  var decisionJson: String?,
  @Enumerated(EnumType.STRING)
  var decision: ChangeRequestDecision?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_change_request_rejection_reason_id")
  val rejectionReason: Cas1ChangeRequestRejectionReasonEntity?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "decision_made_by_user_id")
  val decisionMadeByUser: UserEntity?,
  val decisionMadeAt: OffsetDateTime?,
  val createdAt: OffsetDateTime,
  var updatedAt: OffsetDateTime,
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
