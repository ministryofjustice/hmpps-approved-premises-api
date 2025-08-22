package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Immutable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1RequestForPlacementService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {

  companion object {
    private const val DASHBOARD_SELECT = """
      SELECT
      pq.duration AS requestedPlacementDuration,
      pq.expected_arrival AS requestedPlacementArrivalDate,
      pq.id AS id,
      application.crn AS personCrn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' AS personTier,
      pq.application_id AS applicationId,
      apa.name as personName,
      pq.is_parole as isParole,
      pq.created_at as created_at,
      CASE WHEN (pq.is_parole) THEN 'parole' ELSE 'standardRelease' END AS requestType,
      application.submitted_at::date AS applicationSubmittedDate,
      spaceBookingPremises.name AS bookingPremisesName,
      spaceBooking.canonical_arrival_date AS bookingArrivalDate,
      CASE WHEN spaceBooking.id IS NOT NULL THEN 'matched' WHEN bnm.id IS NOT NULL THEN 'unableToMatch' ELSE 'notMatched' END AS placementRequestStatus
    """

    private const val DASHBOARD_FROM_CLAUSE = """
      FROM
      placement_requests pq
      LEFT JOIN approved_premises_applications apa ON apa.id = pq.application_id
      LEFT JOIN applications application ON application.id = pq.application_id
      LEFT OUTER JOIN LATERAL (
        SELECT id, premises_id, canonical_arrival_date
        FROM cas1_space_bookings b
        WHERE b.placement_request_id = pq.id AND b.cancellation_occurred_at IS NULL
        ORDER BY b.canonical_arrival_date ASC
        LIMIT 1
      ) spaceBooking ON TRUE
      LEFT JOIN premises spaceBookingPremises ON spaceBooking.premises_id = spaceBookingPremises.id
      LEFT JOIN booking_not_mades bnm ON bnm.placement_request_id = pq.id
      WHERE
      (:tier IS NULL OR apa.risk_ratings -> 'tier' -> 'value' ->> 'level' = :tier)      
      AND (CAST(:arrivalDateFrom AS DATE) IS NULL OR pq.expected_arrival >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS DATE) IS NULL OR pq.expected_arrival <= :arrivalDateTo)
      AND (
        :requestType IS NULL OR 
        (
            (:requestType = 'parole' AND pq.is_parole IS TRUE)
            OR
            (:requestType = 'standardRelease' AND pq.is_parole IS FALSE)
        )
      )
      AND (:cruManagementAreaId IS NULL OR apa.cas1_cru_management_area_id = :cruManagementAreaId)
      AND (:crnOrName IS NULL OR (application.crn = UPPER(:crnOrName)) OR (apa.name LIKE UPPER('%' || :crnOrName || '%')))
      AND (:status IS NULL OR pq.is_withdrawn IS FALSE)
      AND (:status IS NULL OR 
        CASE WHEN spaceBooking.id IS NOT NULL THEN 'matched'
            WHEN bnm.id IS NOT NULL THEN 'unableToMatch'
            ELSE 'notMatched' END = :status)    
    """
  }
  fun findByApplicationId(applicationId: UUID): List<PlacementRequestEntity>

  fun findByApplication(application: ApprovedPremisesApplicationEntity): List<PlacementRequestEntity>

  @Query(
    """
    SELECT
      pq.*,
      application.created_at as application_date,
      CASE
        WHEN (pq.is_parole) THEN 'parole'
        ELSE 'standardRequest'
      END as request_type,
      apa.name as person_name,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as person_risks_tier
    from
      placement_requests pq
      left join approved_premises_applications apa on apa.id = pq.application_id
      left join ap_areas area on area.id = apa.ap_area_id
      left join applications application on application.id = pq.application_id
    where
      (:status IS NULL OR pq.is_withdrawn IS FALSE)
      AND (:status IS NULL OR (
        CASE
          WHEN EXISTS (
            SELECT
              1
            from
              cancellations c
              right join bookings booking on c.booking_id = booking.id
            WHERE
              booking.id = pq.booking_id
              AND c.id IS NULL
          ) THEN 'matched'
          WHEN EXISTS (
            SELECT 
                1 
            FROM 
                cas1_space_bookings sb 
            WHERE
                sb.placement_request_id = pq.id AND
                sb.cancellation_occurred_at IS NULL
          ) THEN 'matched'   
          WHEN EXISTS (
            SELECT
              1
            from
              booking_not_mades bnm
            WHERE
              bnm.placement_request_id = pq.id
          ) THEN 'unableToMatch' 
          ELSE 'notMatched'
        END
      ) = :status)
      AND (:crn IS NULL OR EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crn)))
      AND (
        :crnOrName IS NULL OR 
        (
            (EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crnOrName)))
            OR
            (EXISTS (SELECT 1 FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.name LIKE UPPER('%' || :crnOrName || '%')))
        )
      )
      AND (:tier IS NULL OR EXISTS (SELECT 1 FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.risk_ratings -> 'tier' -> 'value' ->> 'level' = :tier)) 
      AND (CAST(:arrivalDateFrom AS date) IS NULL OR pq.expected_arrival >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS date) IS NULL OR pq.expected_arrival <= :arrivalDateTo)
      AND (
        :requestType IS NULL OR 
        (
            (:requestType = 'parole' AND pq.is_parole IS TRUE)
            OR
            (:requestType = 'standardRelease' AND pq.is_parole IS FALSE)
        )
      )
      AND ((CAST(:apAreaId AS pg_catalog.uuid) IS NULL) OR area.id = :apAreaId)
      AND ((CAST(:cruManagementAreaId AS pg_catalog.uuid) IS NULL) OR apa.cas1_cru_management_area_id = :cruManagementAreaId)
  """,
    nativeQuery = true,
  )
  fun allForDashboard(
    status: String? = null,
    crn: String? = null,
    crnOrName: String? = null,
    tier: String? = null,
    arrivalDateFrom: LocalDate? = null,
    arrivalDateTo: LocalDate? = null,
    requestType: String? = null,
    apAreaId: UUID? = null,
    cruManagementAreaId: UUID? = null,
    pageable: Pageable? = null,
  ): Page<PlacementRequestEntity>

  @Query(
    DASHBOARD_SELECT + DASHBOARD_FROM_CLAUSE,
    countQuery = "SELECT COUNT(*) $DASHBOARD_FROM_CLAUSE",
    nativeQuery = true,
  )
  fun allForCas1Dashboard(
    status: String? = null,
    crnOrName: String? = null,
    tier: String? = null,
    arrivalDateFrom: LocalDate? = null,
    arrivalDateTo: LocalDate? = null,
    requestType: String? = null,
    cruManagementAreaId: UUID? = null,
    pageable: Pageable? = null,
  ): Page<Cas1PlacementRequestSummary>
}

/**
 * A [PlacementRequestEntity] is created once a request for placement [PlacementApplicationEntity]
 * is accepted. All space bookings in the system should be tied to a [PlacementRequestEntity]
 * (there are a few legacy bookings that are _not_ linked, see [Cas1SpaceBookingEntity.placementRequest])
 *
 * When the system was originally written, [PlacementApplicationEntity]s were not created for
 * requests for placements made in the original [ApprovedPremisesApplicationEntity], meaning
 * not all [PlacementRequestEntity]s are tied to [PlacementApplicationEntity]s. For more information
 * on this see [isForLegacyInitialRequestForPlacement]
 */
@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id")
  val assessment: ApprovedPremisesAssessmentEntity,

  /**
   * See [isForLegacyInitialRequestForPlacement] for why this is nullable
   */
  @OneToOne
  @JoinColumn(name = "placement_application_id")
  var placementApplication: PlacementApplicationEntity?,

  val createdAt: OffsetDateTime,

  /**
   * Notes from the assessor for the CRU Manager
   */
  val notes: String?,

  @OneToMany(mappedBy = "placementRequest", fetch = FetchType.LAZY)
  var spaceBookings: MutableList<Cas1SpaceBookingEntity>,

  @OneToMany(mappedBy = "placementRequest", fetch = FetchType.LAZY)
  var bookingNotMades: MutableList<BookingNotMadeEntity>,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_requirements_id")
  var placementRequirements: PlacementRequirementsEntity,

  var isParole: Boolean,
  var isWithdrawn: Boolean,

  @Enumerated(value = EnumType.STRING)
  var withdrawalReason: PlacementRequestWithdrawalReason?,

  @Version
  var version: Long = 1,
) {
  fun isInWithdrawableState() = isActive()

  fun hasActiveBooking() = (spaceBookings.any { it.isActive() })

  fun expectedDeparture(): LocalDate = expectedArrival.plusDays(duration.toLong())

  fun isActive() = !isWithdrawn

  /**
   * Before 26/8/25, if a request for placement was made as part of the original
   * application (i.e. an arrival date was defined), a corresponding [PlacementApplicationEntity]
   * wasn't created. Because of this, older [PlacementRequestEntity]s maybe not have
   * a corresponding [PlacementApplicationEntity]
   *
   * As of  26/8/25, we now create a [PlacementApplicationEntity] for all requests
   * for placements made on the original application when the application is
   * authorised and accepted. These placement applications will have a placement
   * type of 'AUTOMATIC'
   *
   * For Withdrawal functionality we have a use-case for the user to be able to withdraw the original
   * request for placement without withdrawing the whole application and/or assessment.
   *
   * Where there isn't a first class entity to represent initial requests for placements
   * (i.e. there isn't an entry in [PlacementApplicationEntity]), we instead use the
   * [PlacementRequestEntity] that was created for these dates to represent the request for placement.
   *
   * Whilst not ideal, this gives us a tangible and addressable entity against which:
   *
   * 1) The user can withdraw the initial request for placement (by including it in response from
   * [uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService])
   * 2) We can raise domain events for request for placement creation and withdrawal
   * 3) We can use to return request for placement information from [Cas1RequestForPlacementService]
   *
   * This property is used to identify such instances
   *
   * Note that we also populate [PlacementApplicationPlaceholderEntity] for any initial request
   * for placements to support reporting on initial requests before a [PlcementRequestEntity]
   * exists
   */
  fun isForLegacyInitialRequestForPlacement() = placementApplication == null
}

@Repository
interface LockablePlacementRequestRepository : JpaRepository<LockablePlacementRequestEntity, UUID> {
  @Query("SELECT a FROM LockablePlacementRequestEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): LockablePlacementRequestEntity?
}

/**
 * Provides a version of the PlacementRequestEntity with no relationships, allowing
 * us to lock the PlacementRequests table only without JPA/Hibernate attempting to
 * lock all eagerly loaded relationships
 */
@Entity
@Table(name = "placement_requests")
@Immutable
class LockablePlacementRequestEntity(
  @Id
  val id: UUID,
)

enum class PlacementRequestWithdrawalReason(val apiValue: WithdrawPlacementRequestReason) {
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
  RELATED_PLACEMENT_APPLICATION_WITHDRAWN(WithdrawPlacementRequestReason.relatedPlacementApplicationWithdrawn),
  ;

  companion object {
    fun valueOf(apiValue: WithdrawPlacementRequestReason): PlacementRequestWithdrawalReason? = PlacementRequestWithdrawalReason.entries.firstOrNull { it.apiValue == apiValue }
  }
}

@SuppressWarnings("TooManyFunctions")
interface Cas1PlacementRequestSummary {
  fun getRequestedPlacementDuration(): Int
  fun getRequestedPlacementArrivalDate(): LocalDate
  fun getId(): UUID
  fun getPersonCrn(): String
  fun getPersonTier(): String?
  fun getApplicationId(): UUID
  fun getPlacementRequestStatus(): PlacementRequestStatus
  fun getApplicationSubmittedDate(): LocalDate
  fun getIsParole(): Boolean
  fun getBookingPremisesName(): String?
  fun getBookingArrivalDate(): LocalDate?
}
