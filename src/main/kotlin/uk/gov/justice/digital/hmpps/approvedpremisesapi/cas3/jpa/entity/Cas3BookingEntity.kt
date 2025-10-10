package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Entity
@Table(name = "bookings")
data class Cas3BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var arrivals: MutableList<Cas3ArrivalEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var departures: MutableList<Cas3DepartureEntity>,
  @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var nonArrival: Cas3NonArrivalEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var cancellations: MutableList<Cas3CancellationEntity>,
  @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
  var confirmation: Cas3v2ConfirmationEntity?,
  @OneToOne
  @JoinColumn(name = "application_id")
  var application: ApplicationEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var extensions: MutableList<Cas3ExtensionEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var dateChanges: MutableList<DateChangeEntity>,
  var service: String,
  var originalArrivalDate: LocalDate,
  var originalDepartureDate: LocalDate,
  val createdAt: OffsetDateTime,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
  var turnarounds: MutableList<Cas3v2TurnaroundEntity>,
  var nomsNumber: String?,
  @Enumerated(value = EnumType.STRING)
  var status: Cas3BookingStatus?,
  @Version
  var version: Long = 1,
  var offenderName: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "premises_id")
  val premises: Cas3PremisesEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bed_id")
  var bedspace: Cas3BedspacesEntity,
) {
  val departure: Cas3DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: Cas3CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val turnaround: Cas3v2TurnaroundEntity?
    get() = turnarounds.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  val arrival: Cas3ArrivalEntity?
    get() = arrivals.maxByOrNull { it.createdAt }

  fun hasNonZeroDayTurnaround() = turnaround != null && turnaround!!.workingDayCount != 0

  fun hasZeroDayTurnaround() = turnaround == null || turnaround!!.workingDayCount == 0

  fun isActive() = !isCancelled

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Cas3BookingEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (arrivalDate != other.arrivalDate) return false
    if (departureDate != other.departureDate) return false
    if (nonArrival != other.nonArrival) return false
    if (confirmation != other.confirmation) return false
    if (originalArrivalDate != other.originalArrivalDate) return false
    if (originalDepartureDate != other.originalDepartureDate) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(
    crn,
    arrivalDate,
    departureDate,
    nonArrival,
    confirmation,
    originalArrivalDate,
    originalDepartureDate,
    createdAt,
  )

  override fun toString() = "Cas3BookingEntity:$id"
}

@Repository
interface Cas3v2BookingRepository : JpaRepository<Cas3BookingEntity, UUID> {
  @Query(
    """
      SELECT b FROM Cas3BookingEntity b
      WHERE b.bedspace.id = :bedspaceId 
      AND NOT EXISTS (SELECT na FROM Cas3NonArrivalEntity na WHERE na.booking = b)
      AND b.arrivalDate <= :date
      AND SIZE(b.cancellations) = 0
      AND (CAST(:excludeBookingId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :excludeBookingId)
    """,
  )
  fun findByBedspaceIdAndArrivingBeforeDate(bedspaceId: UUID, date: LocalDate, excludeBookingId: UUID?): List<Cas3BookingEntity>

  @Query(
    """      
      SELECT
        b.crn AS personCrn,
        b.offender_name AS personName,
        b.id bookingId,
        COALESCE(b.status, 'provisional') as bookingStatus,
        b.arrival_date AS bookingStartDate,
        b.departure_date AS bookingEndDate,
        b.created_at AS bookingCreatedAt,
        p.id premisesId,
        p.name AS premisesName,
        p.address_line1 AS premisesAddressLine1,
        p.address_line2 AS premisesAddressLine2,
        p.town AS premisesTown,
        p.postcode AS premisesPostcode,
        b2.id bedspaceId,
        b2.reference AS bedspaceReference
      FROM bookings b
      LEFT JOIN cas3_bedspaces b2 ON b.bed_id = b2.id
      LEFT JOIN cas3_premises p ON b.premises_id = p.id
      LEFT JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
      WHERE b.service = 'temporary-accommodation'
      AND (:status is null or b.status = :status)
      AND (:probationRegionId is null or pdu.probation_region_id = :probationRegionId)
      AND (:crnOrName is null OR lower(b.crn) = lower(:crnOrName) OR lower(b.offender_name) LIKE CONCAT('%', lower(:crnOrName),'%'))
    """,
    countQuery = """      
      SELECT count(1)
      FROM bookings b
      LEFT JOIN cas3_bedspaces b2 ON b.bed_id = b2.id
      LEFT JOIN cas3_premises p ON b.premises_id = p.id
      LEFT JOIN probation_delivery_units pdu ON p.probation_delivery_unit_id = pdu.id
      WHERE b.service = 'temporary-accommodation'
      AND (:status is null or b.status = :status)
      AND (:probationRegionId is null or pdu.probation_region_id = :probationRegionId)
      AND (:crnOrName is null OR lower(b.crn) = lower(:crnOrName) OR lower(b.offender_name) LIKE CONCAT('%', lower(:crnOrName),'%'))
    """,
    nativeQuery = true,
  )
  fun findBookings(
    status: String?,
    probationRegionId: UUID?,
    crnOrName: String?,
    pageable: Pageable?,
  ): Page<Cas3v2BookingSearchResult>

  @Query("SELECT b FROM Cas3BookingEntity b WHERE b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND b.bedspace = :bedspace")
  fun findAllByOverlappingDateForBedspace(startDate: LocalDate, endDate: LocalDate, bedspace: Cas3BedspacesEntity): List<Cas3BookingEntity>

  @Query(
    """
      SELECT b FROM Cas3BookingEntity b
      JOIN Cas3PremisesEntity p ON b.premises.id = p.id
      WHERE p.id = :premisesId
      AND NOT EXISTS (SELECT na FROM Cas3NonArrivalEntity na WHERE na.booking = b )
      AND b.departureDate >= :date
      AND SIZE(b.cancellations) = 0 
    """,
  )
  fun findActiveOverlappingBookingByPremisesId(premisesId: UUID, date: LocalDate): List<Cas3BookingEntity>

  @Query(
    "SELECT b FROM Cas3BookingEntity b " +
      "WHERE b.bedspace.id = :bedspaceId " +
      "AND NOT EXISTS (SELECT na FROM Cas3NonArrivalEntity na WHERE na.booking = b ) " +
      "AND b.departureDate >= :date " +
      "AND SIZE(b.cancellations) = 0 ",
  )
  fun findActiveOverlappingBookingByBedspace(bedspaceId: UUID, date: LocalDate): List<Cas3BookingEntity>

  /*
  This query is to find the closest booking to the start date for the current bedspace search
  The ClosestBooking is to get the closest booking to the bedspace search start date that is not cancelled
   */
  @Query(
    """
      WITH ClosestBooking AS
         (SELECT b.bed_id, MAX(b.departure_date) departure_date
          FROM bookings b
          LEFT JOIN cancellations c ON b.id = c.booking_id
          WHERE b.departure_date <= :date
            AND b.bed_id IN :bedIds
            AND c.id IS NULL
          GROUP BY b.bed_id)
          
      SELECT b.*,
      cas3_bedspaces.id as bedspace_id,
      cas3_bedspaces.reference as bedspace_reference,
      cas3_bedspaces.created_at as bed_created_at,
      cas3_bedspaces.end_date as bed_end_date
      FROM bookings b
      LEFT JOIN cas3_bedspaces ON b.bed_id = cas3_bedspaces.id  
      INNER JOIN ClosestBooking cb ON b.bed_id = cb.bed_id AND b.departure_date = cb.departure_date
      """,
    nativeQuery = true,
  )
  fun findClosestBookingBeforeDateForBedspaces(date: LocalDate, bedIds: List<UUID>): List<Cas3BookingEntity>

  @Query(
    """
    SELECT
        bk.id as bookingId,
        bk.crn as crn,
        bk.arrival_date as arrivalDate,
        bk.departure_date as departureDate,
        bk.premises_id as premisesId,
        b.id as bedspaceId,
        a.id as assessmentId, 
        CASE 
            WHEN ap.is_registered_sex_offender = TRUE 
            OR ap.is_concerning_sexual_behaviour = TRUE
            OR ap.is_history_of_sexual_offence = TRUE 
         THEN TRUE 
        ELSE FALSE 
        END as sexualRisk
    FROM bookings bk
             INNER JOIN cas3_premises p ON bk.premises_id = p.id
             INNER JOIN cas3_bedspaces b ON bk.bed_id = b.id
             LEFT JOIN temporary_accommodation_applications ap ON bk.application_id = ap.id
             LEFT JOIN assessments a ON ap.id = a.application_id
             LEFT JOIN cancellations c ON bk.id = c.booking_id
    WHERE bk.premises_id IN (:premisesIds) AND bk.arrival_date <= :endDate AND bk.departure_date >= :startDate AND c.id IS NULL
    """,
    nativeQuery = true,
  )
  fun findAllNotCancelledByPremisesIdsAndOverlappingDate(
    premisesIds: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<Cas3v2OverlapBookingsSearchResult>
}

@Suppress("TooManyFunctions")
interface Cas3v2BookingSearchResult {
  fun getPersonName(): String?
  fun getPersonCrn(): String
  fun getBookingStatus(): String
  fun getBookingId(): UUID
  fun getBookingStartDate(): LocalDate
  fun getBookingEndDate(): LocalDate
  fun getBookingCreatedAt(): Instant
  fun getPremisesId(): UUID
  fun getPremisesName(): String
  fun getPremisesAddressLine1(): String
  fun getPremisesAddressLine2(): String?
  fun getPremisesTown(): String?
  fun getPremisesPostcode(): String
  fun getBedspaceId(): UUID
  fun getBedspaceReference(): String
}

interface Cas3v2OverlapBookingsSearchResult {
  val bookingId: UUID
  val crn: String
  val arrivalDate: LocalDate
  val departureDate: LocalDate
  val premisesId: UUID
  val bedspaceId: UUID
  val assessmentId: UUID?
  val sexualRisk: Boolean
}
