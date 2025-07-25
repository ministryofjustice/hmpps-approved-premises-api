package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Immutable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_CHILD_SEX_OFFENDERS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_NON_SEXUAL_CHILD_OFFENDERS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_SEX_OFFENDERS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_CATERED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_ESAP
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_PIPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_PREMISES_SUITABLE_FOR_VULNERABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SUITED_FOR_SEX_OFFENDERS
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.commaSeparatedToList
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1SpaceBookingRepository : JpaRepository<Cas1SpaceBookingEntity, UUID> {
  fun findByPlacementRequestId(placementRequestId: UUID): List<Cas1SpaceBookingEntity>

  companion object {

    /**
     * The following criteria is used to determine if a space booking is 'upcoming'
     *
     * * not arrived
     * * not non-arrived
     * * not cancelled
     * * has a departure date after this constant's value
     *
     * This was introduced because when importing arrival information into CAS1 from
     * delius as part of the find and book rollout, we had several bookings in the
     * past for which there was no arrival or non-arrival recorded.
     *
     * Without this date-based threshold the upcoming tab would include many old bookings
     * and require a substantial amount of manual data cleansing. Use of this simple date
     * threshold was determined to be a preferable approach
     */
    internal const val UPCOMING_EXPECTED_DEPARTURE_THRESHOLD = "2025-01-01"
    val UPCOMING_EXPECTED_DEPARTURE_THRESHOLD_DATE: LocalDate = LocalDate.parse(UPCOMING_EXPECTED_DEPARTURE_THRESHOLD)

    private const val SPACE_BOOKING_SELECT = """
      SELECT 
        CAST(b.id AS varchar) AS id,
        b.crn AS crn,
        b.canonical_arrival_date AS canonicalArrivalDate,
        b.canonical_departure_date AS canonicalDepartureDate,
        b.expected_arrival_date AS expectedArrivalDate,
        b.expected_departure_date AS expectedDepartureDate,
        b.actual_arrival_date AS actualArrivalDate,
        b.actual_arrival_time AS actualArrivalTime,
        b.actual_departure_date AS actualDepartureDate,
        b.actual_departure_time AS actualDepartureTime,
        b.non_arrival_confirmed_at AS nonArrivalConfirmedAtDateTime,
        apa.risk_ratings -> 'tier' -> 'value' ->> 'level' AS tier,
        b.key_worker_staff_code AS keyWorkerStaffCode,
        b.key_worker_assigned_at AS keyWorkerAssignedAt,
        b.key_worker_name AS keyWorkerName,
        CASE 
          WHEN apa.id IS NOT NULL THEN apa.name
          ELSE offline_app.name
        END AS personName,
        b.delius_event_number AS deliusEventNumber,
        b.cancellation_occurred_at IS NOT NULL AS cancelled,
        (
          SELECT STRING_AGG(characteristics.property_name, ',')
          FROM cas1_space_bookings_criteria sbc
          LEFT OUTER JOIN characteristics ON characteristics.id = sbc.characteristic_id
          WHERE sbc.space_booking_id = b.id 
          GROUP BY sbc.space_booking_id
        ) AS characteristicsPropertyNamesCsv,
        (
          SELECT STRING_AGG(DISTINCT change_requests.type, ',')
          FROM cas1_change_requests change_requests
          WHERE change_requests.cas1_space_booking_id = b.id AND change_requests.resolved = false
        ) AS openChangeRequestTypeNamesCsv
    """

    private const val SPACE_BOOKING_SUMMARY_WHERE_CLAUSE = """
      FROM cas1_space_bookings b
      LEFT OUTER JOIN approved_premises_applications apa ON b.approved_premises_application_id = apa.id
      LEFT OUTER JOIN offline_applications offline_app ON b.offline_application_id = offline_app.id
      WHERE 
      b.premises_id = :premisesId AND 
      b.cancellation_occurred_at IS NULL AND 
      (
        cast(:residency as text) IS NULL OR (
          (
            :residency = 'upcoming' AND (
              b.actual_arrival_date IS NULL AND 
              b.non_arrival_confirmed_at IS NULL AND
              b.expected_departure_date >= '$UPCOMING_EXPECTED_DEPARTURE_THRESHOLD'
            )
          ) OR
          (
            :residency = 'current' AND ( 
              b.actual_arrival_date IS NOT NULL AND
              b.non_arrival_confirmed_at IS NULL AND
              b.actual_departure_date IS NULL  AND
              b.expected_departure_date >= '$UPCOMING_EXPECTED_DEPARTURE_THRESHOLD'
            )
          ) OR
          (
            :residency = 'historic' AND 
            (
              b.actual_departure_date IS NOT NULL OR 
              b.non_arrival_confirmed_at IS NOT NULL OR 
              b.expected_departure_date < '$UPCOMING_EXPECTED_DEPARTURE_THRESHOLD'
            )
          )
        ) 
      ) AND
      (
        cast(:crnOrName as text) IS NULL OR 
        (
            (b.crn ILIKE :crnOrName) OR
            ((apa.id IS NOT NULL) AND (apa.name ILIKE '%' || :crnOrName || '%')) OR 
            ((offline_app.id IS NOT NULL) AND (offline_app.name ILIKE '%' || :crnOrName || '%'))
        ) 
      ) AND
      (
        cast(:keyWorkerStaffCode as text) IS NULL OR 
        (
            (b.key_worker_staff_code = :keyWorkerStaffCode)
        ) 
      )
    """

    private const val SPACE_BOOKING_SUMMARY_SELECT_QUERY = """
        $SPACE_BOOKING_SELECT
        $SPACE_BOOKING_SUMMARY_WHERE_CLAUSE
      """

    private const val SPACE_BOOKING_SUMMARY_COUNT_QUERY = """
      SELECT COUNT(*)
      $SPACE_BOOKING_SUMMARY_WHERE_CLAUSE
    """
  }

  @Query(
    value = SPACE_BOOKING_SUMMARY_SELECT_QUERY,
    countQuery = SPACE_BOOKING_SUMMARY_COUNT_QUERY,
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
      b.crn as crn,
      b.canonical_arrival_date as canonicalArrivalDate,
      b.canonical_departure_date as canonicalDepartureDate,
      b.expected_arrival_date as expectedArrivalDate,
      b.expected_departure_date as expectedDepartureDate,
      b.actual_arrival_date as actualArrivalDate,
      b.actual_departure_date as actualDepartureDate,
      b.non_arrival_confirmed_at AS nonArrivalConfirmedAtDateTime,
      b.key_worker_staff_code AS keyWorkerStaffCode,
      b.key_worker_assigned_at AS keyWorkerAssignedAt,
      b.key_worker_name AS keyWorkerName,
    CASE
        WHEN apa.id IS NOT NULL THEN apa.name
        ELSE offline_app.name
        END AS personName,
    b.delius_event_number AS deliusEventNumber,
    b.cancellation_occurred_at IS NOT NULL AS cancelled,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      CASE
        WHEN apa.id IS NOT NULL THEN apa.name
        ELSE offline_app.name
      END as personName,
      apa.release_type as releaseType, 
      ( 
        SELECT STRING_AGG (characteristics.property_name, ',')
        FROM cas1_space_bookings_criteria sbc
        LEFT OUTER JOIN characteristics ON characteristics.id = sbc.characteristic_id
        WHERE sbc.space_booking_id = b.id 
        GROUP by sbc.space_booking_id
      ) AS characteristicsPropertyNamesCsv,
      (
        SELECT STRING_AGG(DISTINCT change_requests.type, ',')
          FROM cas1_change_requests change_requests
          WHERE change_requests.cas1_space_booking_id = b.id AND change_requests.resolved = false
      ) AS openChangeRequestTypeNamesCsv
      FROM cas1_space_bookings b
      LEFT JOIN approved_premises_applications apa ON b.approved_premises_application_id = apa.id
      LEFT JOIN offline_applications offline_app ON b.offline_application_id = offline_app.id
      WHERE 
        b.canonical_arrival_date <= :date AND 
        b.canonical_departure_date > :date AND
        b.premises_id = :premisesId AND 
        b.cancellation_occurred_at IS NULL AND
        b.non_arrival_confirmed_at IS NULL AND
        (:excludeSpaceBookingId IS NULL OR b.id != :excludeSpaceBookingId) AND
        (:criteriaCount = 0 OR 
            (
              SELECT COUNT(*) 
              FROM cas1_space_bookings_criteria sbc
              WHERE b.id = sbc.space_booking_id AND sbc.characteristic_id IN (:criteria)
            ) = :criteriaCount
        )
    """,
    nativeQuery = true,
  )
  fun findSpaceBookingsByPremisesIdAndCriteriaForDate(
    premisesId: UUID,
    date: LocalDate,
    criteria: List<UUID>?,
    criteriaCount: Int = criteria?.size ?: 0,
    excludeSpaceBookingId: UUID?,
    sort: Sort,
  ): List<Cas1SpaceBookingSearchResult>

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
      LIMIT 100
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
    WHERE b.premises.id IN (:premisesIds)
    AND b.cancellationOccurredAt IS NULL 
    AND b.canonicalArrivalDate <= :rangeEndInclusive
    AND b.canonicalDepartureDate >= :rangeStartInclusive 
  """,
  )
  fun findNonCancelledBookingsInRange(
    premisesIds: List<UUID>,
    rangeStartInclusive: LocalDate,
    rangeEndInclusive: LocalDate,
  ): List<Cas1SpaceBookingEntity>

  fun findAllByApplication(application: ApplicationEntity): List<Cas1SpaceBookingEntity>

  @Modifying
  @Query(
    """
    UPDATE Cas1SpaceBookingEntity sb set 
    sb.deliusEventNumber = :eventNumber
    where sb.application.id = :applicationId
    """,
  )
  fun updateEventNumber(applicationId: UUID, eventNumber: String)

  @Query(
    value = """
        SELECT COUNT(*)
        FROM cas1_space_bookings b
        WHERE 
            b.premises_id = :premisesId AND 
            b.canonical_arrival_date <= CURRENT_DATE AND
            b.canonical_departure_date > CURRENT_DATE AND
            b.non_arrival_confirmed_at IS NULL AND 
            b.cancellation_occurred_at IS NULL
    """,
    nativeQuery = true,
  )
  fun countActiveSpaceBookings(premisesId: UUID): Long

  fun findByTransferredFrom(spaceBooking: Cas1SpaceBookingEntity): Cas1SpaceBookingEntity?

  @Query(
    value = """
      SELECT count(*) > 0
      FROM cas1_space_bookings b 
      WHERE b.transferred_from = :spaceBookingId AND b.cancellation_occurred_at IS NULL
      """,
    nativeQuery = true,
  )
  fun hasNonCancelledTransfer(spaceBookingId: UUID): Boolean

  /*
  Checking cancellationRecordedAt shouldn't be required, because an arrived
  booking can't be cancelled. Unfortunately, there are some historical bookings
  with both an arrival and cancellation
   */
  @Query(
    """
    SELECT b FROM Cas1SpaceBookingEntity b 
    INNER JOIN FETCH b.premises
    WHERE 
    b.crn = :crn AND 
    b.actualArrivalDate IS NOT NULL AND 
    b.actualDepartureDate IS NULL AND
    b.cancellationOccurredAt IS NULL AND 
    b.expectedDepartureDate >= :expectedDepartureThreshold
    """,
  )
  fun findResidentSpaceBookingsForCrn(crn: String, expectedDepartureThreshold: LocalDate): List<Cas1SpaceBookingEntity>
}

interface Cas1SpaceBookingSearchResult {
  val id: UUID
  val crn: String
  val canonicalArrivalDate: LocalDate
  val canonicalDepartureDate: LocalDate
  val expectedArrivalDate: LocalDate
  val expectedDepartureDate: LocalDate
  val actualArrivalDate: LocalDate?
  val actualArrivalTime: LocalTime?
  val actualDepartureDate: LocalDate?
  val actualDepartureTime: LocalTime?
  val nonArrivalConfirmedAtDateTime: LocalDateTime?
  val tier: String?
  val keyWorkerStaffCode: String?
  val keyWorkerAssignedAt: Instant?
  val keyWorkerName: String?
  val characteristicsPropertyNamesCsv: String?
  val deliusEventNumber: String?
  val cancelled: Boolean
  val openChangeRequestTypeNamesCsv: String?
}

fun Cas1SpaceBookingSearchResult.getCharacteristicPropertyNames() = characteristicsPropertyNamesCsv.commaSeparatedToList()

fun Cas1SpaceBookingSearchResult.getOpenChangeRequestTypes() = openChangeRequestTypeNamesCsv
  .commaSeparatedToList()
  .map { propertyName -> ChangeRequestType.entries.first { it.name == propertyName } }

interface Cas1SpaceBookingAtPremises {
  val id: UUID
  val canonicalArrivalDate: LocalDate
  val canonicalDepartureDate: LocalDate
}

@Repository
interface LockableCas1SpaceBookingEntityRepository : JpaRepository<LockableCas1SpaceBookingEntity, UUID> {

  @Query("Select sb from LockableCas1SpaceBookingEntity sb where sb.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): LockableCas1SpaceBookingEntity?
}

@Entity
@Table(name = "cas1_space_bookings")
@Immutable
class LockableCas1SpaceBookingEntity(
  @Id
  val id: UUID,
)

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
  val offlineApplication: OfflineApplicationEntity?,
  /**
   * Placement request can be null for the following:
   *
   * 1. A booking linked to an offlineApplication (these are legacy and shouldn't be created anymore)
   * 2. A booking created from a migrated booking, where adhoc was originally true (these are legacy and shouldn't be created anymore)
   * 3. A booking created from a migrated cancelled booking. In this case the legacy data model didn't track placement
   *    request ids for cancelled (and subsequently rebooked) bookings
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_request_id")
  val placementRequest: PlacementRequestEntity?,
  /**
   * createdBy will only be null for migrated [BookingEntity]s where no 'Booking Made' domain event
   * existed for the booking (i.e. those migrated into the system when it went live)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity?,
  val createdAt: OffsetDateTime,
  var expectedArrivalDate: LocalDate,
  var expectedDepartureDate: LocalDate,
  var actualArrivalDate: LocalDate?,
  /**
   * For data imported from delius this can be null even if an actual arrival date has been recorded
   */
  var actualArrivalTime: LocalTime?,
  var actualDepartureDate: LocalDate?,
  /**
   * For data imported from delius this can be null even if an actual departure date has been recorded
   */
  var actualDepartureTime: LocalTime?,
  var canonicalArrivalDate: LocalDate,
  var canonicalDepartureDate: LocalDate,
  val crn: String,
  var keyWorkerStaffCode: String?,
  var keyWorkerName: String?,
  /**
   * For data imported from delius this can be null even if a key worker staff code is set
   */
  var keyWorkerAssignedAt: Instant?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_reason_id")
  var departureReason: DepartureReasonEntity?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "departure_move_on_category_id")
  var departureMoveOnCategory: MoveOnCategoryEntity?,
  var departureNotes: String?,
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
  /**
   * This should be limited to the characteristics with property names defined by
   * [CHARACTERISTICS_OF_INTEREST]
   */
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "cas1_space_bookings_criteria",
    joinColumns = [JoinColumn(name = "space_booking_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  var criteria: MutableList<CharacteristicEntity>,
  var nonArrivalConfirmedAt: Instant?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "non_arrival_reason_id")
  var nonArrivalReason: NonArrivalReasonEntity?,
  var nonArrivalNotes: String?,
  /**
   * All new bookings will have delius event number set, some legacy bookings do not
   * have a value for this (because we didn't initially capture event number for
   * offline applications)
   */
  var deliusEventNumber: String?,
  /**
   * If a value is set, this space booking was migrated from a booking
   */
  @Enumerated(EnumType.STRING)
  val migratedManagementInfoFrom: ManagementInfoSource?,
  /**
   * Delius' internal identifier for this referral.
   *
   * This will only be set for bookings back-filled from referrals created
   * in delius, or those where management information was back-filled from
   * delius when converted from a legacy booking. It is captured for support
   * purposes when debugging migration issues.
   */
  val deliusId: String?,

  /**
   * We don't map transferredTo (i.e. we don't define the bi-directional
   * relationship in the JPA model), because this will _always_ be eager
   * loaded and we don't typically need this information.
   *
   * Instead, use the repository to find the value for 'transferredTo'.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transferred_from", referencedColumnName = "id")
  val transferredFrom: Cas1SpaceBookingEntity? = null,

  /**
   * If [transferredFrom] is not null, this indicates the type of transfer
   */
  @Enumerated(EnumType.STRING)
  var transferType: TransferType? = null,
  @Version
  var version: Long = 1,
) {

  companion object {
    private val PREMISE_CHARACTERISTICS_OF_INTEREST = listOf(
      CAS1_PROPERTY_NAME_PREMISES_PIPE,
      CAS1_PROPERTY_NAME_PREMISES_ESAP,
      CAS1_PROPERTY_NAME_PREMISES_RECOVERY_FOCUSSED,
      CAS1_PROPERTY_NAME_PREMISES_SEMI_SPECIALIST_MENTAL_HEALTH,
      CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_CHILD_SEX_OFFENDERS,
      CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_NON_SEXUAL_CHILD_OFFENDERS,
      CAS1_PROPERTY_NAME_PREMISES_ACCEPTS_SEX_OFFENDERS,
      CAS1_PROPERTY_NAME_PREMISES_CATERED,
      CAS1_PROPERTY_NAME_PREMISES_SUITABLE_FOR_VULNERABLE,
    )

    val ROOM_CHARACTERISTICS_OF_INTEREST = listOf(
      CAS1_PROPERTY_NAME_ARSON_SUITABLE,
      CAS1_PROPERTY_NAME_ENSUITE,
      CAS1_PROPERTY_NAME_SINGLE_ROOM,
      CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
      CAS1_PROPERTY_NAME_SUITED_FOR_SEX_OFFENDERS,
      CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED,
    )

    val CHARACTERISTICS_OF_INTEREST = PREMISE_CHARACTERISTICS_OF_INTEREST + ROOM_CHARACTERISTICS_OF_INTEREST
  }

  @Deprecated("The definition of active is ambiguous, use !isCancelled() instead")
  fun isActive() = !isCancelled()
  fun isCancelled() = cancellationOccurredAt != null
  fun hasDeparted() = actualDepartureDate != null
  fun hasNonArrival() = nonArrivalConfirmedAt != null
  fun hasArrival() = actualArrivalDate != null
  fun isExpectedOrResident(day: LocalDate) = !isCancelled() &&
    !hasNonArrival() &&
    canonicalArrivalDate <= day &&
    canonicalDepartureDate > day

  override fun toString() = "Cas1SpaceBookingEntity:$id"
  val applicationFacade: Cas1ApplicationFacade
    get() = Cas1ApplicationFacade(application, offlineApplication)
}

enum class ManagementInfoSource {
  DELIUS,
  LEGACY_CAS_1,
}

enum class TransferType {
  PLANNED,
  EMERGENCY,
}
