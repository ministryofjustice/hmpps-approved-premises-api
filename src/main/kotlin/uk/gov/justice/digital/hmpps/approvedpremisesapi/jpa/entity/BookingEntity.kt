package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface BookingRepository : JpaRepository<BookingEntity, UUID>

@Entity
@Table(name = "bookings")
data class BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  @ManyToOne
  @JoinColumn(name = "key_worker_id")
  var keyWorker: KeyWorkerEntity,
  @OneToOne(mappedBy = "booking")
  var arrival: ArrivalEntity?,
  @OneToOne(mappedBy = "booking")
  var departure: DepartureEntity?,
  @OneToOne(mappedBy = "booking")
  var nonArrival: NonArrivalEntity?,
  @OneToOne(mappedBy = "booking")
  var cancellation: CancellationEntity?,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BookingEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (arrivalDate != other.arrivalDate) return false
    if (departureDate != other.departureDate) return false
    if (keyWorker != other.keyWorker) return false
    if (arrival != other.arrival) return false
    if (departure != other.departure) return false
    if (nonArrival != other.nonArrival) return false
    if (cancellation != other.cancellation) return false

    return true
  }

  override fun hashCode() = Objects.hash(crn, arrivalDate, departureDate, keyWorker, arrival, departure, nonArrival, cancellation)

  override fun toString() = "BookingEntity:$id"
}
