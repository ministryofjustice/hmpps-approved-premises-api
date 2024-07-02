package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.BookingSummaryForAvailability
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Version

@Repository
interface BookingRepository : JpaRepository<BookingEntity, UUID> {
  @Query(
    """
    SELECT
      b.arrival_date as arrivalDate,
      b.departure_date as departureDate,
      (
        SELECT
          count(1)
        from
          arrivals
        where
          booking_id = b.id
      ) > 0 as arrived,
      (
        SELECT
          count(1)
        from
          non_arrivals
        where
          booking_id = b.id
      ) > 0 as isNotArrived,
      (
        SELECT
          count(1)
        from
          cancellations
        where
          booking_id = b.id
      ) > 0 as cancelled
    from
      bookings b
    WHERE b.premises_id = :premisesId AND b.arrival_date <= :endDate AND b.departure_date >= :startDate
  """,
    nativeQuery = true,
  )
  fun findAllByPremisesIdAndOverlappingDate(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<BookingSummaryForAvailability>

  @Query("SELECT b FROM BookingEntity b WHERE b.premises.id IN :premisesIds AND b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND SIZE(b.cancellations) = 0")
  fun findAllNotCancelledByPremisesIdsAndOverlappingDate(
    premisesIds: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<BookingEntity>

  @Query("SELECT b FROM BookingEntity b WHERE b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND b.bed = :bed")
  fun findAllByOverlappingDateForBed(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<BookingEntity>

  @Query("SELECT MAX(b.departureDate) FROM BookingEntity b WHERE b.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?

  @Query("SELECT b FROM BookingEntity b WHERE b.bed.id = :bedId AND b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND SIZE(b.cancellations) = 0 AND (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :thisEntityId)")
  fun findByBedIdAndOverlappingDate(
    bedId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
  ): List<BookingEntity>

  @Query("SELECT DISTINCT(b.crn) FROM BookingEntity b")
  fun getDistinctCrns(): List<String>

  @Modifying
  @Query(
    """
    UPDATE bookings SET 
    noms_number = :nomsNumber
    WHERE UPPER(crn) = UPPER(:crn) AND UPPER(noms_number) = UPPER(:oldNomsNumber)
    """,
    nativeQuery = true,
  )
  fun updateNomsByCrn(crn: String, oldNomsNumber: String, nomsNumber: String): Int

  @Query("SELECT DISTINCT(b.nomsNumber) FROM BookingEntity b WHERE b.nomsNumber IS NOT NULL")
  fun getDistinctNomsNumbers(): List<String>

  @Query("SELECT b FROM BookingEntity b WHERE b.bed.id IN :bedIds")
  fun findByBedIds(bedIds: List<UUID>): List<BookingEntity>

  /*
  * There is a bug in Hibernate that has been around since 2010. It is the reason why we have this awful line
  *
  * AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b )

  * * https://hibernate.atlassian.net/browse/HHH-4795

  * * https://stackoverflow.com/questions/52839973/hql-to-check-for-null-in-onetoone-relation
  * */
  @Query(
    "SELECT b FROM BookingEntity b " +
      "WHERE b.bed.id = :bedId " +
      "AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b ) " +
      "AND b.arrivalDate <= :date " +
      "AND SIZE(b.cancellations) = 0 " +
      "AND (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :thisEntityId)",
  )
  fun findByBedIdAndArrivingBeforeDate(bedId: UUID, date: LocalDate, thisEntityId: UUID?): List<BookingEntity>

  @Query(
    "SELECT b FROM BookingEntity b WHERE (b.bed, b.departureDate) IN (" +
      "  SELECT b2.bed, MAX(b2.departureDate)" +
      "  FROM BookingEntity b2 " +
      "  WHERE b2.departureDate <= :date " +
      "  AND b2.bed.id IN :bedIds " +
      "  AND SIZE(b2.cancellations) = 0 " +
      "  GROUP BY b2.bed " +
      ")",
  )
  fun findClosestBookingBeforeDateForBeds(date: LocalDate, bedIds: List<UUID>): List<BookingEntity>

  fun findAllByCrn(crn: String): List<BookingEntity>

  @Query(
    "SELECT b From BookingEntity b WHERE b.application = :application AND (b.adhoc IS TRUE or b.adhoc IS NULL)",
  )
  fun findAllAdhocOrUnknownByApplication(application: ApplicationEntity): List<BookingEntity>

  fun findAllByApplication(application: ApplicationEntity): List<BookingEntity>

  @Query(
    """
      SELECT
        b.crn AS personCrn,
        Cast(b.id as varchar) bookingId,
        CASE 
              WHEN :serviceName='approved-premises' THEN COALESCE(b.status, 'awaiting-arrival')
              ELSE COALESCE(b.status, 'provisional')
        END as bookingStatus,
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
      LEFT JOIN beds b2 ON b.bed_id = b2.id
      LEFT JOIN rooms r ON b2.room_id = r.id
      LEFT JOIN premises p ON r.premises_id = p.id
      WHERE b.service = :serviceName
      AND (:status is null or b.status = :#{#status?.toString()})
      AND (Cast(:probationRegionId as varchar) is null or p.probation_region_id = :probationRegionId)
      AND (:crn is null OR b.crn = :crn)
    """,
    nativeQuery = true,
  )
  fun findBookings(
    serviceName: String,
    status: BookingStatus?,
    probationRegionId: UUID?,
    crn: String?,
    pageable: Pageable?,
  ): Page<BookingSearchResult>

  @Modifying
  @Query("UPDATE BookingEntity b set b.status = :status where b.id = :bookingId")
  fun updateBookingStatus(bookingId: UUID, status: BookingStatus)

  @Query(
    "SELECT * FROM bookings WHERE status IS NULL AND service='temporary-accommodation' ",
    nativeQuery = true,
  )
  fun findAllCas3bookingsWithNullStatus(pageable: Pageable?): Slice<BookingEntity>

  @Query(
    "SELECT b FROM BookingEntity b " +
      "WHERE b.bed.id = :bedId " +
      "AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b ) " +
      "AND b.departureDate >= :date " +
      "AND SIZE(b.cancellations) = 0 ",
  )
  fun findActiveOverlappingBookingByBed(bedId: UUID, date: LocalDate): List<BookingEntity>

  @Modifying
  @Query("UPDATE BookingEntity b set b.adhoc = :adhoc where b.id = :bookingId")
  fun updateBookingAdhocStatus(bookingId: UUID, adhoc: Boolean): Int
}

@Entity
@Table(name = "bookings")
data class BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  var keyWorkerStaffCode: String?,
  @OneToMany(mappedBy = "booking")
  var arrivals: MutableList<ArrivalEntity>,
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
  @Enumerated(value = EnumType.STRING)
  var status: BookingStatus?,
  val adhoc: Boolean? = null,
  @Version
  var version: Long = 1,
) {
  val departure: DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val turnaround: TurnaroundEntity?
    get() = turnarounds.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  val arrival: ArrivalEntity?
    get() = arrivals.maxByOrNull { it.createdAt }

  fun isInCancellableStateCas1() = !isCancelled && !hasArrivals()

  fun isActive() = !isCancelled

  fun hasArrivals() = arrivals.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BookingEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (arrivalDate != other.arrivalDate) return false
    if (departureDate != other.departureDate) return false
    if (keyWorkerStaffCode != other.keyWorkerStaffCode) return false
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
    keyWorkerStaffCode,
    nonArrival,
    confirmation,
    originalArrivalDate,
    originalDepartureDate,
    createdAt,
  )

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
