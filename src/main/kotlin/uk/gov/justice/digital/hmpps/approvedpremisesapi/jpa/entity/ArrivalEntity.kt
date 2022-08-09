package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import java.time.LocalDate
import java.util.Objects
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "arrival")
data class ArrivalEntity(
  @Id
  val id: UUID,
  val arrivalDate: LocalDate,
  val expectedDepartureDate: LocalDate,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ArrivalEntity) return false

    if (id != other.id) return false
    if (arrivalDate != other.arrivalDate) return false
    if (expectedDepartureDate != other.expectedDepartureDate) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(arrivalDate, expectedDepartureDate, notes)

  override fun toString() = "ArrivalEntity:$id"
}
