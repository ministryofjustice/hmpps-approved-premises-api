package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.NamedNativeQuery
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingRecord
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@NamedNativeQuery(
  name = "BookingEntity.getBookingsV2",
  query = """
      SELECT 
          b.bed_id AS bedId,
          b.arrival_date AS arrivalDate,
          b.departure_date AS departureDate,
          t.working_day_count AS turnaroundDays
      FROM bookings b
      LEFT JOIN cancellations c ON c.booking_id = b.id
      LEFT JOIN (
          SELECT DISTINCT ON (booking_id) booking_id, working_day_count
          FROM cas3_turnarounds
          ORDER BY booking_id, created_at DESC
      ) t ON t.booking_id = b.id
      WHERE b.service = 'temporary-accommodation' 
        AND c.id IS NULL 
        AND b.arrival_date <= :endDate 
        AND b.departure_date >= :startDate
    """,
  resultSetMapping = "BookingRecordMapping",
)
@SqlResultSetMapping(
  name = "BookingRecordMapping",
  classes = [
    ConstructorResult(
      targetClass = BookingRecord::class,
      columns = [
        ColumnResult(name = "bedId", type = UUID::class),
        ColumnResult(name = "arrivalDate", type = LocalDate::class),
        ColumnResult(name = "departureDate", type = LocalDate::class),
        ColumnResult(name = "turnaroundDays", type = Int::class),
      ],
    ),
  ],
)
@Entity
@Table(name = "bookings")
@Deprecated(message = "This is a legacy entity. Use CAS3BookingEntity for CAS3 and Cas1SpaceBookingEntity for CAS1")
data class BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  @Deprecated(message = "This is a legacy CAS1-only field that is no longer captured and will be removed once bookings have been fully migrated to space bookings")
  var keyWorkerStaffCode: String?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var arrivals: MutableList<ArrivalEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var departures: MutableList<DepartureEntity>,
  @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var nonArrival: NonArrivalEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var cancellations: MutableList<CancellationEntity>,
  @OneToOne
  @JoinColumn(name = "application_id")
  var application: ApplicationEntity?,
  @OneToOne
  @JoinColumn(name = "offline_application_id")
  var offlineApplication: OfflineApplicationEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var extensions: MutableList<ExtensionEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
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
  var nomsNumber: String?,
  @Enumerated(value = EnumType.STRING)
  var status: BookingStatus?,
  val adhoc: Boolean? = null,
  @Version
  var version: Long = 1,
  var offenderName: String?,
) {
  val departure: DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  val arrival: ArrivalEntity?
    get() = arrivals.maxByOrNull { it.createdAt }

  val cas1ApplicationFacade: Cas1ApplicationFacade
    get() {
      if (offlineApplication == null && application !is ApprovedPremisesApplicationEntity) {
        error("Can only return CAS1 application facade for bookings linked to CAS1 applications")
      }
      return Cas1ApplicationFacade(application as ApprovedPremisesApplicationEntity?, offlineApplication)
    }

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
    originalArrivalDate,
    originalDepartureDate,
    createdAt,
  )

  override fun toString() = "BookingEntity:$id"
}

interface BookingSummaryForAvailability {
  fun getArrivalDate(): LocalDate
  fun getDepartureDate(): LocalDate
  fun getArrived(): Boolean
  fun getIsNotArrived(): Boolean
  fun getCancelled(): Boolean
}
