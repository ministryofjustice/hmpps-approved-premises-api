package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1SpaceBookingRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  fun findByPremisesIdAndPlacementRequestId(premisesId: UUID, placementRequestId: UUID): Cas1SpaceBookingEntity?

  @Query(
    value = """
      SELECT 
      Cast(b.id as varchar),
      b.crn as crn,
      b.canonical_arrival_date as canonicalArrivalDate,
      b.canonical_departure_date as canonicalDepartureDate,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      b.key_worker_staff_code as keyWorkerStaffCode,
      b.key_worker_assigned_at as keyWorkerAssignedAt,
      b.key_worker_name as keyWorkerName,
      apa.name as personName
      FROM cas1_space_bookings b
      INNER JOIN approved_premises_applications apa ON b.approved_premises_application_id = apa.id
      WHERE 
      b.premises_id = :premisesId AND 
      b.cancellation_occurred_at IS NULL AND 
      b.actual_departure_date_time IS NULL AND
      (
        :residency IS NULL OR (
          (:residency = 'upcoming' AND b.actual_arrival_date_time IS NULL) OR
          (:residency = 'current' AND b.actual_arrival_date_time IS NOT NULL and b.actual_departure_date_time IS NULL)
        ) 
      ) AND
      (
        :crnOrName IS NULL OR 
        (
            (b.crn ILIKE :crnOrName) OR
            (apa.name ILIKE '%' || :crnOrName || '%')
        ) 
      )
    """,
    nativeQuery = true,
  )
  fun search(
    residency: String?,
    crnOrName: String?,
    premisesId: UUID,
    pageable: Pageable?,
  ): Page<Cas1SpaceBookingSearchResult>

  @Query(
    value = """
      SELECT 
      Cast(b.id as varchar),
      b.canonical_arrival_date as canonicalArrivalDate,
      b.canonical_departure_date as canonicalDepartureDate
      FROM cas1_space_bookings b
      WHERE 
      b.premises_id = :premisesId AND 
      b.crn = :crn AND
      b.cancellation_occurred_at IS NULL 
    """,
    nativeQuery = true,
  )
  fun findByPremisesIdAndCrn(
    premisesId: UUID,
    crn: String,
  ): List<Cas1SpaceBookingAtPremises>
}

interface Cas1SpaceBookingSearchResult {
  val id: UUID
  val crn: String
  val canonicalArrivalDate: LocalDate
  val canonicalDepartureDate: LocalDate
  val tier: String?
  val keyWorkerStaffCode: String?
  val keyWorkerAssignedAt: Instant?
  val keyWorkerName: String?
}

interface Cas1SpaceBookingAtPremises {
  val id: UUID
  val canonicalArrivalDate: LocalDate
  val canonicalDepartureDate: LocalDate
}

@Entity
@Table(name = "cas1_space_bookings")
data class Cas1SpaceBookingEntity(
  @Id
  val id: UUID,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "premises_id")
  val premises: ApprovedPremisesEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_premises_application_id")
  val application: ApprovedPremisesApplicationEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity,
  val createdAt: OffsetDateTime,
  val expectedArrivalDate: LocalDate,
  var expectedDepartureDate: LocalDate,
  var actualArrivalDateTime: Instant?,
  var actualDepartureDateTime: Instant?,
  var canonicalArrivalDate: LocalDate,
  var canonicalDepartureDate: LocalDate,
  val crn: String,
  var keyWorkerStaffCode: String?,
  var keyWorkerName: String?,
  var keyWorkerAssignedAt: Instant?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_reason_id")
  var departureReason: DepartureReasonEntity?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_move_on_category_id")
  var departureMoveOnCategory: MoveOnCategoryEntity?,
  /**
   * Users are asked to specify when a cancelled occurred which may not necessarily
   * be the same as when it was recorded in the system
   */
  var cancellationOccurredAt: LocalDate?,
  var cancellationRecordedAt: Instant?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cancellation_reason_id")
  var cancellationReason: CancellationReasonEntity?,
  var cancellationReasonNotes: String?,
) {
  fun isCancelled() = cancellationOccurredAt != null
  fun hasArrival() = actualArrivalDateTime != null
}
