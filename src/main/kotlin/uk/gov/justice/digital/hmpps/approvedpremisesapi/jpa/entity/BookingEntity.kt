package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
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

  @Query("SELECT b FROM BookingEntity b WHERE b.arrivalDate <= :endDate AND b.departureDate >= :startDate")
  @Query(
    """
    SELECT
        booking.crn,
        app.id,
        app.submitted_at,
        cas3_app.risk_ratings,
        cas3_app.is_registered_sex_offender,
        cas3_app.needs_accessible_property,
        cas3_app.has_history_of_arson,
        cas3_app.is_duty_to_refer_submitted,
        cas3_app.duty_to_refer_submission_date,
        cas3_app.is_eligible,
        cas3_app.eligibility_reason,
        probation_region.name,
        arr.arrival_date,
        arr.expected_departure_date,
        confirmation.id,
        departure.date_time,
        cancellation.id,
        cancellation_reason.name,
        move_on_category.name
    FROM
        bookings booking
    LEFT JOIN
        premises premises ON premises.id = booking.premises_id
    LEFT JOIN
        probation_regions probation_region ON probation_region.id = premises.probation_region_id
    LEFT JOIN
        applications app ON booking.application_id = app.id
    LEFT JOIN
        temporary_accommodation_applications cas3_app ON booking.application_id = cas3_app.id
    LEFT JOIN
        confirmations confirmation ON confirmation.booking_id = booking.id
    LEFT JOIN
        departures departure ON departure.booking_id = booking.id
    LEFT JOIN
        arrivals arr ON arr.booking_id = booking.id
    LEFT JOIN
        cancellations cancellation ON cancellation.booking_id = booking.id
    LEFT JOIN
        cancellation_reasons cancellation_reason ON cancellation_reason.id = cancellation.cancellation_reason_id
    LEFT JOIN
        move_on_categories move_on_category ON move_on_category.id = departure.move_on_category_id
    WHERE
        booking.arrival_date <= :endDate
        AND booking.departure_date >= :startDate
    """,
    nativeQuery = true
  )
  fun findAllByOverlappingDate(startDate: LocalDate, endDate: LocalDate): List<BookingEntity>

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
