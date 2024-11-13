package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1SpaceBookingRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  fun findByPremisesIdAndPlacementRequestId(premisesId: UUID, placementRequestId: UUID): Cas1SpaceBookingEntity?

  fun deleteByPremisesIdAndMigratedFromBookingIsNotNull(premisesId: UUID): Long

  @Query(
    value = """
      SELECT 
      Cast(b.id as varchar),
      b.crn as crn,
      b.canonical_arrival_date as canonicalArrivalDate,
      b.canonical_departure_date as canonicalDepartureDate,
      b.expected_arrival_date as expectedArrivalDate,
      b.expected_departure_date as expectedDepartureDate,
      b.actual_arrival_date_time as actualArrivalDateTime,
      b.actual_departure_date_time as actualDepartureDateTime,
      b.non_arrival_confirmed_at as nonArrivalConfirmedAtDateTime,
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
      (
        cast(:residency as text) IS NULL OR (
          (:residency = 'upcoming' AND b.actual_arrival_date_time IS NULL AND b.non_arrival_confirmed_at IS NULL) OR
          (:residency = 'current' AND b.actual_arrival_date_time IS NOT NULL AND 
            b.actual_departure_date_time IS NULL AND b.non_arrival_confirmed_at IS NULL) OR
          (:residency = 'historic' AND 
            (b.actual_departure_date_time IS NOT NULL OR b.non_arrival_confirmed_at IS NOT NULL)
          )
        ) 
      ) AND
      (
        cast(:crnOrName as text) IS NULL OR 
        (
            (b.crn ILIKE :crnOrName) OR
            (apa.name ILIKE '%' || :crnOrName || '%')
        ) 
      ) AND
      (
        cast(:keyWorkerStaffCode as text) IS NULL OR 
        (
            (b.key_worker_staff_code = :keyWorkerStaffCode)
        ) 
      )
    """,
    nativeQuery = true,
  )
  fun search(
    residency: String?,
    crnOrName: String?,
    keyWorkerStaffCode: String?,
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

  @Query(
    value =
    """
    SELECT b FROM Cas1SpaceBookingEntity b
    LEFT JOIN FETCH b.criteria
    WHERE b.premises.id = :premisesId
    AND b.cancellationOccurredAt IS NULL 
    AND b.canonicalArrivalDate <= :day 
    AND b.canonicalDepartureDate > :day
  """,
  )
  fun findAllBookingsOnGivenDayWithCriteria(premisesId: UUID, day: LocalDate): List<Cas1SpaceBookingEntity>
}

interface Cas1SpaceBookingSearchResult {
  val id: UUID
  val crn: String
  val canonicalArrivalDate: LocalDate
  val canonicalDepartureDate: LocalDate
  val expectedArrivalDate: LocalDate
  val expectedDepartureDate: LocalDate
  val actualArrivalDateTime: LocalDateTime?
  val actualDepartureDateTime: LocalDateTime?
  val nonArrivalConfirmedAtDateTime: LocalDateTime?
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
  /**
   * A booking will always be associated with either an [ApprovedPremisesApplicationEntity], or
   * an [OfflineApplicationEntity]. All new bookings will only be associated with an
   * [ApprovedPremisesApplicationEntity]
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_premises_application_id")
  val application: ApprovedPremisesApplicationEntity?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offline_application_id")
  var offlineApplication: OfflineApplicationEntity?,
  /**
   * Placement request will only be null for migrated [BookingEntity]s, where adhoc = true
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity?,
  /**
   * createdAt will only be null for migrated [BookingEntity]s where no 'Booking Made' domain event
   * existed for the booking (i.e. those migrated into the system when it went live)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity?,
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
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "cas1_space_bookings_criteria",
    joinColumns = [JoinColumn(name = "space_booking_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val criteria: List<CharacteristicEntity>,
  var nonArrivalConfirmedAt: Instant?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "non_arrival_reason_id")
  var nonArrivalReason: NonArrivalReasonEntity?,
  var nonArrivalNotes: String?,
  val deliusEventNumber: String?,
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "migrated_from_booking_id")
  val migratedFromBooking: BookingEntity?,
  @Enumerated(EnumType.STRING)
  val migratedManagementInfoFrom: ManagementInfoSource?,
) {
  fun isActive() = !isCancelled()
  fun isCancelled() = cancellationOccurredAt != null
  fun hasNonArrival() = nonArrivalConfirmedAt != null
  fun hasArrival() = actualArrivalDateTime != null
  override fun toString() = "Cas1SpaceBookingEntity:$id"
}

enum class ManagementInfoSource {
  DELIUS,
  LEGACY_CAS_1,
}
