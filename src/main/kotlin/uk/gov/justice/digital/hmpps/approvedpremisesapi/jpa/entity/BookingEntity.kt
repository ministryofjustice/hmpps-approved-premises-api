package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.listeners.BookingListener
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface BookingRepository : JpaRepository<BookingEntity, UUID> {
  @Query("SELECT b FROM BookingEntity b WHERE b.premises.id = :premisesId AND b.arrivalDate <= :endDate AND b.departureDate >= :startDate")
  fun findAllByPremisesIdAndOverlappingDate(premisesId: UUID, startDate: LocalDate, endDate: LocalDate): List<BookingEntity>

  @Query("SELECT b FROM BookingEntity b WHERE b.premises.id IN :premisesIds AND b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND SIZE(b.cancellations) = 0")
  fun findAllNotCancelledByPremisesIdsAndOverlappingDate(premisesIds: List<UUID>, startDate: LocalDate, endDate: LocalDate): List<BookingEntity>

  @Query("SELECT b FROM BookingEntity b WHERE b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND b.bed = :bed")
  fun findAllByOverlappingDateForBed(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<BookingEntity>

  @Query("SELECT MAX(b.departureDate) FROM BookingEntity b WHERE b.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?

  @Query("SELECT b FROM BookingEntity b WHERE b.bed.id = :bedId AND b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND SIZE(b.cancellations) = 0 AND (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :thisEntityId)")
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<BookingEntity>

  @Query("SELECT DISTINCT(b.crn) FROM BookingEntity b")
  fun getDistinctCrns(): List<String>

  @Query("SELECT DISTINCT(b.nomsNumber) FROM BookingEntity b WHERE b.nomsNumber IS NOT NULL")
  fun getDistinctNomsNumbers(): List<String>

  @Query("SELECT b FROM BookingEntity b WHERE b.bed.id IN :bedIds")
  fun findByBedIds(bedIds: List<UUID>): List<BookingEntity>

  @Query(
    "SELECT b FROM BookingEntity b " +
      "WHERE b.bed.id = :bedId " +
      "AND b.arrivalDate <= :date " +
      "AND SIZE(b.cancellations) = 0 " +
      "AND (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :thisEntityId)",
  )
  fun findByBedIdAndArrivingBeforeDate(bedId: UUID, date: LocalDate, thisEntityId: UUID?): List<BookingEntity>

  @Query(
    "SELECT b FROM BookingEntity b WHERE (b.bed, b.departureDate) IN (" +
      "  SELECT b2.bed, MAX(b2.departureDate)" +
      "  FROM BookingEntity b2 " +
      "  WHERE b2.departureDate < :date " +
      "  AND b2.bed.id IN :bedIds " +
      "  AND SIZE(b2.cancellations) = 0 " +
      "  GROUP BY b2.bed " +
      ")",
  )
  fun findClosestBookingBeforeDateForBeds(date: LocalDate, bedIds: List<UUID>): List<BookingEntity>

  fun findAllByCrn(crn: String): List<BookingEntity>

  fun findByApplication(application: ApplicationEntity): BookingEntity

  @Query(
    """
      SELECT
        b.crn AS personCrn,
        Cast(b.id as varchar) bookingId,
        s.booking_status AS bookingStatus,
        b.arrival_date AS bookingStartDate,
        b.departure_date AS bookingEndDate,
        b.created_at AS bookingCreatedAt,
        Cast(p.id as varchar) premisesId,
        p.name AS premisesName,
        p.address_line1 AS premisesAddressLine1,
        p.address_line2 AS premisesAddressLine2,
        p.town AS premisesTown,
        p.postcode AS premisesPostcode,
        Cast(r.id as varchar) roomId,
        r.name AS roomName,
        Cast(b2.id as varchar) bedId,
        b2.name AS bedName
      FROM bookings b
      LEFT JOIN (
        SELECT
          b.id,
          (
            CASE
              WHEN (SELECT COUNT(1) FROM cancellations c WHERE c.booking_id = b.id) > 0 THEN 'cancelled'
              WHEN (SELECT COUNT(1) FROM departures d WHERE d.booking_id = b.id) > 0 THEN 'departed'
              WHEN (SELECT COUNT(1) FROM arrivals a WHERE a.booking_id = b.id) > 0 THEN 'arrived'
              WHEN (SELECT COUNT(1) FROM confirmations c2 WHERE c2.booking_id = b.id) > 0 THEN 'confirmed'
              WHEN (SELECT COUNT(1) FROM non_arrivals n WHERE n.booking_id = n.id) > 0 THEN 'not-arrived'
              WHEN :serviceName = 'approved-premises' THEN 'awaiting-arrival'
              ELSE 'provisional'
            END
          ) AS booking_status
        FROM bookings b
      ) as s ON b.id = s.id
      LEFT JOIN beds b2 ON b.bed_id = b2.id
      LEFT JOIN rooms r ON b2.room_id = r.id
      LEFT JOIN premises p ON r.premises_id = p.id
      WHERE b.service = :serviceName
      AND (:status is null or s.booking_status = :#{#status?.toString()})
      AND (Cast(:probationRegionId as varchar) is null or p.probation_region_id = :probationRegionId)
    """,
    nativeQuery = true,
  )
  fun findBookings(serviceName: String, status: BookingStatus?, probationRegionId: UUID?, pageable: Pageable?): Page<BookingSearchResult>
}

@EntityListeners(BookingListener::class)
@Entity
@Table(name = "bookings")
data class BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  var keyWorkerStaffCode: String?,
  @OneToOne(mappedBy = "booking")
  var arrival: ArrivalEntity?,
  @OneToMany(mappedBy = "booking")
  var departures: MutableList<DepartureEntity>,
  @OneToOne(mappedBy = "booking")
  var nonArrival: NonArrivalEntity?,
  @OneToMany(mappedBy = "booking")
  var cancellations: MutableList<CancellationEntity>,
  @OneToOne(mappedBy = "booking")
  var confirmation: ConfirmationEntity?,
  @OneToOne
  @JoinColumn(name = "application_id")
  var application: ApplicationEntity?,
  @OneToOne
  @JoinColumn(name = "offline_application_id")
  var offlineApplication: OfflineApplicationEntity?,
  @OneToMany(mappedBy = "booking")
  var extensions: MutableList<ExtensionEntity>,
  @OneToMany(mappedBy = "booking")
  var dateChanges: MutableList<DateChangeEntity>,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bed: BedEntity?,
  var service: String,
  var originalArrivalDate: LocalDate,
  var originalDepartureDate: LocalDate,
  val createdAt: OffsetDateTime,
  @OneToMany(mappedBy = "booking")
  var turnarounds: MutableList<TurnaroundEntity>,
  var nomsNumber: String?,
  @OneToOne(mappedBy = "booking")
  var placementRequest: PlacementRequestEntity?,
) {
  val departure: DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val turnaround: TurnaroundEntity?
    get() = turnarounds.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BookingEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (arrivalDate != other.arrivalDate) return false
    if (departureDate != other.departureDate) return false
    if (keyWorkerStaffCode != other.keyWorkerStaffCode) return false
    if (arrival != other.arrival) return false
    if (nonArrival != other.nonArrival) return false
    if (confirmation != other.confirmation) return false
    if (originalArrivalDate != other.originalArrivalDate) return false
    if (originalDepartureDate != other.originalDepartureDate) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(crn, arrivalDate, departureDate, keyWorkerStaffCode, arrival, nonArrival, confirmation, originalArrivalDate, originalDepartureDate, createdAt)

  override fun toString() = "BookingEntity:$id"
}

@Suppress("TooManyFunctions")
interface BookingSearchResult {
  fun getPersonName(): String?
  fun getPersonCrn(): String
  fun getBookingStatus(): String
  fun getBookingId(): UUID
  fun getBookingStartDate(): LocalDate
  fun getBookingEndDate(): LocalDate
  fun getBookingCreatedAt(): Timestamp
  fun getPremisesId(): UUID
  fun getPremisesName(): String
  fun getPremisesAddressLine1(): String
  fun getPremisesAddressLine2(): String?
  fun getPremisesTown(): String?
  fun getPremisesPostcode(): String
  fun getRoomId(): UUID
  fun getRoomName(): String
  fun getBedId(): UUID
  fun getBedName(): String
}
