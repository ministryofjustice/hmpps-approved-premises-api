package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Entity
@Table(name = "cancellations")
data class CancellationEntity(
  @Id
  val id: UUID,
  val date: LocalDate,
  @ManyToOne
  @JoinColumn(name = "cancellation_reason_id")
  val reason: CancellationReasonEntity,
  val notes: String?,
  val createdAt: OffsetDateTime,
  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity,
  val otherReason: String?,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CancellationEntity) return false

    if (id != other.id) return false
    if (date != other.date) return false
    if (reason != other.reason) return false
    if (notes != other.notes) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(date, reason, notes, createdAt)

  override fun toString() = "CancellationEntity:$id"
}
