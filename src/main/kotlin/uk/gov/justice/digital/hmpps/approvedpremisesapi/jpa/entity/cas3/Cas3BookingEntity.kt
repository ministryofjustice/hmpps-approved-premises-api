package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "bookings")
@Inheritance(strategy = InheritanceType.JOINED)
class Cas3BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var arrivals: MutableList<ArrivalEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var departures: MutableList<DepartureEntity>,
  @OneToOne(mappedBy = "booking", cascade = [ CascadeType.REMOVE ])
  var nonArrival: NonArrivalEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var cancellations: MutableList<CancellationEntity>,
  @OneToOne(mappedBy = "booking")
  var confirmation: Cas3ConfirmationEntity?,
  @OneToOne
  @JoinColumn(name = "application_id")
  var application: ApplicationEntity?,
  @OneToOne
  @JoinColumn(name = "offline_application_id")
  var offlineApplication: OfflineApplicationEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var extensions: MutableList<ExtensionEntity>,
  var service: String,
  var originalArrivalDate: LocalDate,
  var originalDepartureDate: LocalDate,
  val createdAt: OffsetDateTime,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
  var turnarounds: MutableList<Cas3TurnaroundEntity>,
  var nomsNumber: String?,
  @OneToOne(mappedBy = "booking")
  var placementRequest: PlacementRequestEntity?,
  @Enumerated(value = EnumType.STRING)
  var status: BookingStatus?,
  val adhoc: Boolean? = null,
  @Version
  var version: Long = 1,
  var offenderName: String?,

  @ManyToOne
  @JoinColumn(name = "premises_id")
  val cas3Premises: Cas3PremisesEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bedspace: Cas3BedspacesEntity?,
) {
  val departure: DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val turnaround: Cas3TurnaroundEntity?
    get() = turnarounds.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  val arrival: ArrivalEntity?
    get() = arrivals.maxByOrNull { it.createdAt }

  fun isActive() = !isCancelled
}

@Repository
interface Cas3BookingRepository : JpaRepository<Cas3BookingEntity, UUID>
