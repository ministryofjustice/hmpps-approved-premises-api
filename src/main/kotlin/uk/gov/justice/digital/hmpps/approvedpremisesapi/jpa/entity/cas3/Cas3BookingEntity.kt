package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
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
  @OneToOne(fetch = FetchType.LAZY)
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
    """,
  )
  fun findByBedspaceIdAndArrivingBeforeDate(bedspaceId: UUID, date: LocalDate): List<Cas3BookingEntity>

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
