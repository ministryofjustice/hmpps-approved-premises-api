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
@Table(name = "cancellation")
data class CancellationEntity(
  @Id
  val id: UUID,
  val date: LocalDate,
  val reason: String,
  val notes: String?,
  @OneToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CancellationEntity) return false

    if (id != other.id) return false
    if (date != other.date) return false
    if (reason != other.reason) return false
    if (notes != other.notes) return false

    return true
  }

  override fun hashCode() = Objects.hash(date, reason, notes)

  override fun toString() = "CancellationEntity:$id"
}
