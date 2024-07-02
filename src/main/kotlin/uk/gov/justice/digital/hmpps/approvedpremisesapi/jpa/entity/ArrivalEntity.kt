package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface ArrivalRepository : JpaRepository<ArrivalEntity, UUID>

@Entity
@Table(name = "arrivals")
data class ArrivalEntity(
  @Id
  val id: UUID,
  val arrivalDate: LocalDate,
  val arrivalDateTime: Instant,
  val expectedDepartureDate: LocalDate,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ArrivalEntity) return false

    if (id != other.id) return false
    if (arrivalDate != other.arrivalDate) return false
    if (expectedDepartureDate != other.expectedDepartureDate) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(arrivalDate, expectedDepartureDate, notes, createdAt)

  override fun toString() = "ArrivalEntity:$id"
}
