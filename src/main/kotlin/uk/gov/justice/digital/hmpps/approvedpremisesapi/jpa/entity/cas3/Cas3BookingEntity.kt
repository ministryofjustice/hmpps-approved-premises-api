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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.v2.Cas3v2TurnaroundEntity
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
  var application: TemporaryAccommodationApplicationEntity?,
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
  var status: BookingStatus?,
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
