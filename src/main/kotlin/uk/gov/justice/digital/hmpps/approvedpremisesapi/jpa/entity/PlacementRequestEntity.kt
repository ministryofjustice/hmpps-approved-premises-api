package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): PlacementRequestEntity?

  fun findAllByApplication(application: ApplicationEntity): List<PlacementRequestEntity>

  fun findAllByPlacementApplication(placementApplication: PlacementApplicationEntity): List<PlacementRequestEntity>

  fun findAllByAllocatedToUser_IdAndReallocatedAtNullAndIsWithdrawnFalse(
    userId: UUID,
    pageable: Pageable?,
  ): Page<PlacementRequestEntity>

  @Query(
    """
    SELECT
      placement_request.*,
      booking_not_made.id
    FROM
      placement_requests placement_request
      left join booking_not_mades booking_not_made on booking_not_made.placement_request_id = placement_request.id
    where
      placement_request.booking_id IS NULL
      AND placement_request.reallocated_at IS NULL
      AND placement_request.is_withdrawn is false
      AND booking_not_made.id IS NULL
    """,
    nativeQuery = true,
  )
  fun findAllReallocatable(pageable: Pageable?): Page<PlacementRequestEntity>

  @Query(
    """
    SELECT
      placement_request.*,
      booking_not_made.id
    FROM
      placement_requests placement_request
      left join booking_not_mades booking_not_made on booking_not_made.placement_request_id = placement_request.id
    where
      placement_request.booking_id IS NULL
      AND placement_request.reallocated_at IS NULL
      AND placement_request.is_withdrawn is false
      AND booking_not_made.id IS NULL
      AND placement_request.allocated_to_user_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllReallocatableAllocated(pageable: Pageable?): Page<PlacementRequestEntity>

  @Query(
    """
    SELECT
      placement_request.*,
      booking_not_made.id
    FROM
      placement_requests placement_request
      left join booking_not_mades booking_not_made on booking_not_made.placement_request_id = placement_request.id
    where
      placement_request.booking_id IS NULL
      AND placement_request.reallocated_at IS NULL
      AND placement_request.is_withdrawn is false
      AND booking_not_made.id IS NULL
      AND placement_request.allocated_to_user_id IS NULL
    """,
    nativeQuery = true,
  )
  fun findAllReallocatableUnallocated(pageable: Pageable?): Page<PlacementRequestEntity>

  fun findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(
    isParole: Boolean,
    pageable: Pageable?,
  ): Page<PlacementRequestEntity>

  @Query(
    """
    SELECT
      pq.*,
      application.created_at as application_date
    from
      placement_requests pq
      left join applications application on application.id = pq.application_id
      left join approved_premises_applications apa on apa.id = application.id
    where
      pq.reallocated_at IS NULL 
      AND (:status IS NULL OR pq.is_withdrawn IS FALSE)
      AND (:status IS NULL OR (
        CASE
          WHEN (
            SELECT
              COUNT(booking)
            from
              bookings booking
              left join cancellations c on c.booking_id = booking.id
            WHERE
              booking.id = pq.booking_id
              AND c.id IS NULL
          ) > 0 THEN 'matched'
          WHEN (
            SELECT
              COUNT(bnm)
            from
              booking_not_mades bnm
            WHERE
              bnm.placement_request_id = pq.id
          ) > 0 THEN 'unableToMatch'
          ELSE 'notMatched'
        END
      ) = :#{#status?.toString()})
      AND (:crn IS NULL OR (SELECT COUNT(1) FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crn)) = 1)
      AND (
        :crnOrName IS NULL OR 
        (
            ((SELECT COUNT(1) FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crnOrName)) = 1)
            OR
            ((SELECT COUNT(1) FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.name LIKE UPPER('%' || :crnOrName || '%')) = 1)
        )
      )
      AND (:tier IS NULL OR (SELECT COUNT(1) FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.risk_ratings -> 'tier' -> 'value' ->> 'level' = :tier) = 1) 
      AND (CAST(:arrivalDateFrom AS date) IS NULL OR pq.expected_arrival >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS date) IS NULL OR pq.expected_arrival <= :arrivalDateTo)
      AND (CAST(:probationRegionId AS pg_catalog.uuid) IS NULL OR apa.probation_region_id = :probationRegionId)
  """,
    nativeQuery = true,
  )
  fun allForDashboard(
    status: PlacementRequestStatus?,
    crn: String?,
    crnOrName: String?,
    tier: String?,
    arrivalDateFrom: LocalDate?,
    arrivalDateTo: LocalDate?,
    pageable: Pageable?,
    probationRegionId: UUID? = null,
  ): Page<PlacementRequestEntity>
}

@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  @OneToOne
  @JoinColumn(name = "placement_application_id")
  val placementApplication: PlacementApplicationEntity?,

  val createdAt: OffsetDateTime,

  val notes: String?,

  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  val allocatedToUser: UserEntity?,

  @OneToMany(mappedBy = "placementRequest")
  var bookingNotMades: MutableList<BookingNotMadeEntity>,

  var reallocatedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "placement_requirements_id")
  var placementRequirements: PlacementRequirementsEntity,

  var isParole: Boolean,
  var isWithdrawn: Boolean,
)
